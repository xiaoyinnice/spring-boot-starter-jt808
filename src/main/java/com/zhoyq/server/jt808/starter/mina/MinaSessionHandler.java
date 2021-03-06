/*
 *  Copyright (c) 2020. 刘路 All rights reserved
 *  版权所有 刘路 并保留所有权利 2020.
 *  ============================================================================
 *  这不是一个自由软件！您只能在不用于商业目的的前提下对程序代码进行修改和
 *  使用。不允许对程序代码以任何形式任何目的的再发布。如果项目发布携带作者
 *  认可的特殊 LICENSE 则按照 LICENSE 执行，废除上面内容。请保留原作者信息。
 *  ============================================================================
 *  刘路（feedback@zhoyq.com）于 2020. 创建
 *  http://zhoyq.com
 */

package com.zhoyq.server.jt808.starter.mina;

import com.zhoyq.server.jt808.starter.config.Jt808Config;
import com.zhoyq.server.jt808.starter.core.Jt808Pack;
import com.zhoyq.server.jt808.starter.core.PackHandler;
import com.zhoyq.server.jt808.starter.helper.ByteArrHelper;
import com.zhoyq.server.jt808.starter.helper.Jt808Helper;
import com.zhoyq.server.jt808.starter.helper.ResHelper;
import com.zhoyq.server.jt808.starter.pack.NoSupportHandler;
import com.zhoyq.server.jt808.starter.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话处理器
 * @author zhoyq <a href="mailto:feedback@zhoyq.com">feedback@zhoyq.com</a>
 * @date 2020/2/16
 */
@Slf4j
@Component
public class MinaSessionHandler extends IoHandlerAdapter implements ApplicationContextAware {
    /**
     * 所有实现的包处理器
     */
    private static Map<Integer, PackHandler> packHandlerMap;

    /**
     * 不支持的协议消息
     */
    @Autowired
    private NoSupportHandler noSupportHandler;

    /**
     * 唤醒时 初始化 packHandlerMap
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(packHandlerMap == null){
            packHandlerMap = new ConcurrentHashMap<>();

            Map<String, Object> handlers =  applicationContext.getBeansWithAnnotation(Jt808Pack.class);
            for (Map.Entry<String, Object> entry : handlers.entrySet()) {
                Object handler = entry.getValue();
                if (handler instanceof PackHandler) {
                    Jt808Pack packConfig = handler.getClass().getAnnotation(Jt808Pack.class);
                    // 新定义的包解析器会替换旧的
                    packHandlerMap.put(packConfig.msgId() ,(PackHandler)handler);
                }
            }
        }
    }

    /**
     * 配置
     */
    @Autowired
    private Jt808Config jt808Config;

    /**
     * 会话空闲
     */
    @Override
    public void sessionIdle(IoSession session, IdleStatus idleStatus){
        log.debug("session idle width {} ", session.getId());
        // 一定时间后未收到信息 关闭链接
        if(session.getIdleCount( idleStatus ) > jt808Config.getIdleCount()){
            session.closeNow();
        }
    }

    /**
     * 异常处理
     */
    @Override
    public void exceptionCaught(IoSession session, Throwable throwable) {
        log.debug("session exception with {}", session.getId());
        log.warn(throwable.getMessage());
        // 异常时 关闭session 等待重连
        session.closeNow();
    }

    /**
     * 会话服务
     */
    @Autowired
    private SessionService sessionService;

    /**
     * 获取消息
     */
    @Override
    public void messageReceived(IoSession session, Object msg) {
        byte[] originData  = (byte[])msg;
        log.debug("session received msg with id {} and msg {}", session.getId(), Arrays.toString(originData));

        // 按照协议解析获得的字节数组 pkgNum 从 1 开始
        byte[] msgId, msgBodyProp, phoneNum, streamNum, pkgCount = null, pkgNum = null, res = null;
        int offset = 0;
        msgId = new byte[]{originData[offset++],originData[offset++]};
        msgBodyProp = new byte[]{originData[offset++],originData[offset++]};
        phoneNum = new byte[]{originData[offset++],originData[offset++],originData[offset++],
                originData[offset++],originData[offset++],originData[offset++]};
        streamNum = new byte[]{originData[offset++],originData[offset++]};
        boolean hasPackage = Jt808Helper.hasPackage(msgBodyProp);
        if( hasPackage ){
            pkgCount = new byte[]{originData[offset++],originData[offset++]};
            pkgNum = new byte[]{originData[offset++],originData[offset]};
        }

        String phone = ByteArrHelper.toHexString(phoneNum);

        // 相同身份的终端建立链接 原链接需要断开 也就是加入之前需要判断是否存在终端 存在关闭后在加入
        if(sessionService.contains(phone)){
            IoSession preSession = (IoSession) sessionService.get(phone);
            if (preSession.getId() != session.getId()) {
                preSession.closeNow();
            }
        }

        // session 加入会话缓存
        sessionService.set(phone, session);
        if( hasPackage ){
            int totalPkgNum = ByteArrHelper.twobyte2int(pkgCount);
            int currentPkgNum = ByteArrHelper.twobyte2int(pkgNum);
            // 序号必须小于等于总包数 条件达成之后进行分包处理 否则不处理分包且不处理数据
            if(totalPkgNum >= currentPkgNum){
                if(!sessionService.containsPackages(phone)){
                    ConcurrentHashMap<Integer,byte[]> buf = new ConcurrentHashMap<>(totalPkgNum);
                    sessionService.setPackages(phone, buf);
                }
                Map<Integer,byte[]> pkgBuf = sessionService.getPackages(phone);
                pkgBuf.put(currentPkgNum,originData);
            }
            // 分包结束时需要对分包数据进行解析处理并返回应答 通过总包数和序号对比 判断是不是最后一包
            if( totalPkgNum == currentPkgNum ){
                // 如果是 这个电话的最后一包
                if(Jt808Helper.pkgAllReveived(sessionService, phone, totalPkgNum)){
                    // 合并所有包 并解析
                    res = data(Jt808Helper.allPkg(sessionService, phone,totalPkgNum));
                }else{
                    // 没有全部收到 需要补传 最初一包的流水号
                    byte[] originStreamNum = null;
                    // 补传id列表
                    byte[] idList = new byte[]{};
                    // 补传数量
                    byte num = 0;
                    Map<Integer,byte[]> map = sessionService.getPackages(phone);
                    for(int i = 1;i<=totalPkgNum;i++){
                        if(originStreamNum == null){
                            originStreamNum = ByteArrHelper.subByte(map.get(1), 10, 12);
                        }
                        if(!map.containsKey(i)){
                            num++;
                            idList = ByteArrHelper.union(idList, ByteArrHelper.subByte(map.get(i), 14, 16));
                        }
                    }
                    if(originStreamNum != null) {
                        res = ResHelper.getPkgReq(phoneNum, originStreamNum, num, idList);
                    }
                }
            }
        }else{
            res =  data(originData);
        }
        if( res == null ){
            res = ResHelper.getPlatAnswer(phoneNum, streamNum, msgId, (byte) 0x00);
        }
        // 分包消息总长度
        int msgLen = jt808Config.getPackageLength();
        if( res.length > msgLen ){
            // 分包发送
            Jt808Helper.sentByPkg(res,session);
        }else{
            Jt808Helper.addPlatStreamNum();
            session.write(res);
        }
    }

    /**
     * 数据处理
     * @param originData 原始数据 有分包的情况已经处理完
     * @return 应答数据
     */
    private byte[] data(byte[] originData){

        byte[] msgBody;

        int offset = 0;

        final byte[] msgId = new byte[]{originData[offset++],originData[offset++]};
        offset++;offset++;
        // 不再解析消息头
        // byte[] msgBodyProp = null;
        // msgBodyProp = new byte[]{originData[offset++],originData[offset++]};
        final byte[] phoneNum = new byte[]{originData[offset++],originData[offset++],originData[offset++],
                originData[offset++],originData[offset++],originData[offset++]};
        final byte[] streamNum = new byte[]{originData[offset++],originData[offset++]};
        int msgLen = jt808Config.getPackageLength();
        if(originData.length > msgLen){
            // 超长的数据一定是分包合并后的数据 直接获取后边的数据即可 因为已经处理了尾部的校验位
            msgBody = ByteArrHelper.subByte(originData, offset);
        }else{
            int bodyLength = originData.length-1-offset;
            msgBody = new byte[bodyLength];
            for(int i=0;i<msgBody.length;i++){
                msgBody[i] = originData[offset++];
            }
        }

        String phone = ByteArrHelper.toHexString(phoneNum);

        // 检查鉴权记录 看是否链接后鉴权过 成功鉴权才能继续访问其他命令
        boolean isAuth;
        // 每次都判断终端鉴权 有些短连接如果每次鉴权 的话 很麻烦 所以推荐使用长链接Map
        isAuth = sessionService.containsAuth(phone);

        int msgIdInt = ByteArrHelper.twobyte2int(msgId);

        // 如果已经鉴权 则可以使用所有命令 未鉴权 则只能使用 终端注册 终端鉴权两个命令
        int terminalRegisterCode = 0x0100;
        int terminalAuthentication = 0x0102;
        if (!isAuth && msgIdInt != terminalRegisterCode && msgIdInt != terminalAuthentication ) {
            return ResHelper.getPlatAnswer(phoneNum, streamNum, msgId, (byte) 0x01);
        }

        PackHandler handler ;
        if (packHandlerMap.containsKey(msgIdInt)) {
            handler = packHandlerMap.get(msgIdInt);
        } else {
            handler = noSupportHandler;
        }
        return handler.handle(phoneNum, streamNum, msgId, msgBody);
    }
}
