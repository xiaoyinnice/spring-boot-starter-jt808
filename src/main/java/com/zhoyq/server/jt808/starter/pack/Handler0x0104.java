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

package com.zhoyq.server.jt808.starter.pack;

import com.zhoyq.server.jt808.starter.core.Jt808Pack;
import com.zhoyq.server.jt808.starter.core.PackHandler;
import com.zhoyq.server.jt808.starter.helper.ByteArrHelper;
import com.zhoyq.server.jt808.starter.helper.ResHelper;
import com.zhoyq.server.jt808.starter.service.DataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 查询终端参数应答
 * @author 刘路 <a href="mailto:feedback@zhoyq.com">feedback@zhoyq.com</a>
 * @date 2018/7/31
 */
@Slf4j
@Jt808Pack(msgId = 0x0104)
public class Handler0x0104 implements PackHandler {
    @Autowired
    private DataService dataService;
    @Autowired
    private ThreadPoolExecutor tpe;
    @Override
    public byte[] handle( byte[] phoneNum, byte[] streamNum, byte[] msgId, byte[] msgBody) {
        log.info("0104 查询终端参数应答 SearchTerminalParameterAnswer");
        int platformStreamNumber = ByteArrHelper.twobyte2int(ByteArrHelper.subByte( msgBody, 0, 2));
        String phone = ByteArrHelper.toHexString(phoneNum);
        // 保存下发指令对应应答里 需要时取消息体进行分析
        tpe.execute(() -> dataService.terminalAnswer(phone, platformStreamNumber, "8104", "0104", msgBody));
        return ResHelper.getPlatAnswer(phoneNum, streamNum, msgId, (byte) 0x00);
    }
}
