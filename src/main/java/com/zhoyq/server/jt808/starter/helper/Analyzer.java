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

package com.zhoyq.server.jt808.starter.helper;

import com.zhoyq.server.jt808.starter.config.Const;
import com.zhoyq.server.jt808.starter.entity.*;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhoyq <a href="mailto:feedback@zhoyq.com">feedback@zhoyq.com</a>
 * @date 2020/2/18
 */
@Slf4j
public class Analyzer {
    /**
     * 回传终端参数分析
     * @param processor 处理器
     * @param data 终端参数项列表
     */
    public static void analyzeParameter(TerminalParameterProcessor processor, byte[] data){
        if (data == null) {
            return ;
        }
        int pos = 0;
        while(pos < data.length){
            byte length = data[pos + 4];

            byte[] id = ByteArrHelper.subByte(data, pos, pos + 4);
            byte[] dataBuf = ByteArrHelper.subByte(data, pos + 5, pos + length + 5);

            pos += length + 5;

            processor.process(ByteArrHelper.toHexString(id), dataBuf);
        }
    }

    /**
     * 分析终端属性消息
     * @param data 二进制数据
     * @return 返回分析后的对象
     */
    public static TerminalProperty analyzeTerminalProperty(byte[] data){
        TerminalProperty terminalProperty = new TerminalProperty();
        // 终端类型
        byte[] terminalType = ByteArrHelper.subByte(data, 0, 2);
        terminalProperty.setSupportBus((terminalType[1] & Const.BIN_0X01) == Const.BIN_0X01);
        terminalProperty.setSupportDangerVehicle((terminalType[1] & Const.BIN_0X02) == Const.BIN_0X02);
        terminalProperty.setSupportFreightVehicle((terminalType[1] & Const.BIN_0X04) == Const.BIN_0X04);
        terminalProperty.setSupportTaxi((terminalType[1] & Const.BIN_0X08) == Const.BIN_0X08);
        terminalProperty.setSupportRecording((terminalType[1] & Const.BIN_0X40) == Const.BIN_0X40);
        terminalProperty.setSupportExtension((terminalType[1] & Const.BIN_0X80) == Const.BIN_0X80);
        // 制造商ID
        byte[] manufacturer = ByteArrHelper.subByte(data, 2, 7);
        terminalProperty.setManufacturer(ByteArrHelper.toHexString(manufacturer));
        // 终端型号
        byte[] terminalModel = ByteArrHelper.subByte(data, 7, 27);
        terminalProperty.setTerminalModel(ByteArrHelper.toHexString(terminalModel));
        // 终端ID
        byte[] terminalId = ByteArrHelper.subByte(data, 27, 34);
        try {
            terminalProperty.setTerminalId(Jt808Helper.toGB18030String(terminalId));
        } catch (UnsupportedEncodingException e) {
            log.warn(e.getMessage());
        }
        // 终端SIM卡ICCID
        byte[] terminalIccid = ByteArrHelper.subByte(data, 34, 44);
        terminalProperty.setIccid(ByteArrHelper.getBCDStrByArr(terminalIccid));
        // 终端硬件版本号长度
        int terminalHardwareVersionLength = data[44];
        // 终端已经按版本号
        byte[] terminalHardwareVersion = ByteArrHelper.subByte(data, 45, 45 + terminalHardwareVersionLength);
        try {
            terminalProperty.setTerminalHardVersion(Jt808Helper.toGB18030String(terminalHardwareVersion));
        } catch (UnsupportedEncodingException e) {
            log.warn(e.getMessage());
        }
        // 终端软件版本号长度
        int terminalSoftwareVersionLength = data[45 + terminalHardwareVersionLength];
        // 终端固件版本号
        byte[] terminalSoftwareVersion = ByteArrHelper.subByte(data, 46 + terminalHardwareVersionLength,
                46 + terminalHardwareVersionLength + terminalSoftwareVersionLength);
        try {
            terminalProperty.setTerminalSoftVersion(Jt808Helper.toGB18030String(terminalSoftwareVersion));
        } catch (UnsupportedEncodingException e) {
            log.warn(e.getMessage());
        }
        // GNSS模块属性
        byte gnssModuleProp = data[46 + terminalHardwareVersionLength + terminalSoftwareVersionLength];
        terminalProperty.setSupportGps((gnssModuleProp & Const.BIN_0X01) == Const.BIN_0X01);
        terminalProperty.setSupportBeidou((gnssModuleProp & Const.BIN_0X02) == Const.BIN_0X02);
        terminalProperty.setSupportGlonass((gnssModuleProp & Const.BIN_0X04) == Const.BIN_0X04);
        terminalProperty.setSupportGalileo((gnssModuleProp & Const.BIN_0X08) == Const.BIN_0X08);
        // 通信模块属性
        byte connectModuleProp = data[47 + terminalHardwareVersionLength + terminalSoftwareVersionLength];
        terminalProperty.setSupportGprs((connectModuleProp & Const.BIN_0X01) == Const.BIN_0X01);
        terminalProperty.setSupportCdma((connectModuleProp & Const.BIN_0X02) == Const.BIN_0X02);
        terminalProperty.setSupportTdscdma((connectModuleProp & Const.BIN_0X04) == Const.BIN_0X04);
        terminalProperty.setSupportWcdma((connectModuleProp & Const.BIN_0X08) == Const.BIN_0X08);
        terminalProperty.setSupportCdma2000((connectModuleProp & Const.BIN_0X10) == Const.BIN_0X10);
        terminalProperty.setSupportTdlte((connectModuleProp & Const.BIN_0X20) == Const.BIN_0X20);
        terminalProperty.setSupportOther((connectModuleProp & Const.BIN_0X80) == Const.BIN_0X80);
        return terminalProperty;
    }

    /**
     * 分析报警信息
     * @param alarms 报警信息
     * @return 报警对象
     */
    public static AlarmInfo analyzeAlarm(byte[] alarms) {
        AlarmInfo alarmInfo = new AlarmInfo();
        alarmInfo.setEmergencyAlarm((alarms[4] & Const.BIN_0X01) == Const.BIN_0X01);
        alarmInfo.setOverSpeedAlarm((alarms[4] & Const.BIN_0X02) == Const.BIN_0X02);
        alarmInfo.setFatigueDrivingAlarm((alarms[4] & Const.BIN_0X04) == Const.BIN_0X04);
        alarmInfo.setDangerWarning((alarms[4] & Const.BIN_0X08) == Const.BIN_0X08);
        alarmInfo.setGnssModuleFault((alarms[4] & Const.BIN_0X10) == Const.BIN_0X10);
        alarmInfo.setGnssConnectFault((alarms[4] & Const.BIN_0X20) == Const.BIN_0X20);
        alarmInfo.setGnssShortCircuit((alarms[4] & Const.BIN_0X40) == Const.BIN_0X40);
        alarmInfo.setPowerUnderpressure((alarms[4] & Const.BIN_0X80) == Const.BIN_0X80);
        alarmInfo.setPowerFault((alarms[3] & Const.BIN_0X01) == Const.BIN_0X01);
        alarmInfo.setLcdFault((alarms[3] & Const.BIN_0X02) == Const.BIN_0X02);
        alarmInfo.setTtsFault((alarms[3] & Const.BIN_0X04) == Const.BIN_0X04);
        alarmInfo.setCameraFault((alarms[3] & Const.BIN_0X08) == Const.BIN_0X08);
        alarmInfo.setIcModuleFault((alarms[3] & Const.BIN_0X10) == Const.BIN_0X10);
        alarmInfo.setOverSpeedWarn((alarms[3] & Const.BIN_0X20) == Const.BIN_0X20);
        alarmInfo.setFatigueDrivingWarn((alarms[3] & Const.BIN_0X40) == Const.BIN_0X40);
        // 15 - 17 保留
        alarmInfo.setCumulativeDrivingTimeout((alarms[2] & Const.BIN_0X04) == Const.BIN_0X04);
        alarmInfo.setStopTimeout((alarms[2] & Const.BIN_0X08) == Const.BIN_0X08);
        alarmInfo.setInArea((alarms[2] & Const.BIN_0X10) == Const.BIN_0X10);
        alarmInfo.setOutLine((alarms[2] & Const.BIN_0X20) == Const.BIN_0X20);
        alarmInfo.setDrivingTimeIncorrect((alarms[2] & Const.BIN_0X40) == Const.BIN_0X40);
        alarmInfo.setRouteDeviation((alarms[2] & Const.BIN_0X80) == Const.BIN_0X80);
        alarmInfo.setVssFault((alarms[1] & Const.BIN_0X01) == Const.BIN_0X01);
        alarmInfo.setOilFault((alarms[1] & Const.BIN_0X02) == Const.BIN_0X02);
        alarmInfo.setStolenVehicle((alarms[1] & Const.BIN_0X04) == Const.BIN_0X04);
        alarmInfo.setIllegalIgnition((alarms[1] & Const.BIN_0X08) == Const.BIN_0X08);
        alarmInfo.setIllegalDisplacement((alarms[1] & Const.BIN_0X10) == Const.BIN_0X10);
        alarmInfo.setCollisionWarn((alarms[1] & Const.BIN_0X20) == Const.BIN_0X20);
        alarmInfo.setRollOverWarn((alarms[1] & Const.BIN_0X40) == Const.BIN_0X40);
        alarmInfo.setIllegalOpeningTheDoor((alarms[1] & Const.BIN_0X80) == Const.BIN_0X80);
        return alarmInfo;
    }

    /**
     * 分析状态信息
     * @param status 状态信息
     * @return 状态对象
     */
    public static StatusInfo analyzeStatus(byte[] status) {
        StatusInfo statusInfo = new StatusInfo();
        statusInfo.setAcc((status[4] & Const.BIN_0X01) == Const.BIN_0X01);
        statusInfo.setPositioning((status[4] & Const.BIN_0X02) == Const.BIN_0X02);
        statusInfo.setSouth((status[4] & Const.BIN_0X04) == Const.BIN_0X04);
        statusInfo.setWest((status[4] & Const.BIN_0X08) == Const.BIN_0X08);
        statusInfo.setSuspended((status[4] & Const.BIN_0X10) == Const.BIN_0X10);
        statusInfo.setEncryption((status[4] & Const.BIN_0X20) == Const.BIN_0X20);
        // 6 - 7 保留
        statusInfo.setCargo(status[3] & 0x03);
        statusInfo.setOilBreak((status[3] & Const.BIN_0X04) == Const.BIN_0X04);
        statusInfo.setCircuitBreak((status[3] & Const.BIN_0X08) == Const.BIN_0X08);
        statusInfo.setLocking((status[3] & Const.BIN_0X10) == Const.BIN_0X10);
        statusInfo.setOpening1((status[3] & Const.BIN_0X20) == Const.BIN_0X20);
        statusInfo.setOpening2((status[3] & Const.BIN_0X40) == Const.BIN_0X40);
        statusInfo.setOpening3((status[3] & Const.BIN_0X80) == Const.BIN_0X80);
        statusInfo.setOpening4((status[2] & Const.BIN_0X01) == Const.BIN_0X01);
        statusInfo.setOpening5((status[2] & Const.BIN_0X02) == Const.BIN_0X02);
        statusInfo.setGps((status[2] & Const.BIN_0X04) == Const.BIN_0X04);
        statusInfo.setBeidou((status[2] & Const.BIN_0X08) == Const.BIN_0X08);
        statusInfo.setGlonass((status[2] & Const.BIN_0X10) == Const.BIN_0X10);
        statusInfo.setGalileo((status[2] & Const.BIN_0X20) == Const.BIN_0X20);
        // 22 - 31 保留
        return statusInfo;
    }

    /**
     * 定位附属信息
     * @param processor 处理器
     * @param attache 附属信息
     */
    public static void analyzeAttache(LocationAttacheProcessor processor, byte[] attache){
        if (attache == null) {
            return ;
        }
        int pos = 0;
        while(pos < attache.length){
            byte length = attache[pos + 1];

            int id = attache[pos];
            byte[] dataBuf = ByteArrHelper.subByte(attache, pos + 2, pos + length + 2);

            pos += length + 2;

            processor.process(id, dataBuf);
        }
    }

    /**
     * 分析定位信息
     * @param msgBody 定位消息体
     * @return 定位信息
     */
    public static LocationInfo analyzeLocation(byte[] msgBody) {
        // 报警标识
        byte[] alarms = ByteArrHelper.subByte(msgBody, 0, 4);
        // 状态
        byte[] status = ByteArrHelper.subByte(msgBody, 4, 8);
        // 纬度[以度为单位的值乘以 10 的 6 次方，精确到百万分之一度]
        byte[] latitude = ByteArrHelper.subByte(msgBody, 8, 12);
        // 经度[以度为单位的值乘以 10 的 6 次方，精确到百万分之一度]
        byte[] longitude = ByteArrHelper.subByte(msgBody, 12, 16);
        // 高程 [单位 m]
        byte[] height = ByteArrHelper.subByte(msgBody, 16, 18);
        // 速度 [单位 0.1 km/h]
        byte[] speed = ByteArrHelper.subByte(msgBody, 18, 20);
        // 方向 [0~359 正北为0 顺时针]
        byte[] direction = ByteArrHelper.subByte(msgBody, 20, 22);
        // 时间 [yy-mm-dd-hh-mm-ss]
        byte[] datetime = ByteArrHelper.subByte(msgBody, 22, 28);
        // 附加
        byte[] attache = ByteArrHelper.subByte(msgBody, 28);

        AlarmInfo alarmInfo = Analyzer.analyzeAlarm(alarms);
        StatusInfo statusInfo = Analyzer.analyzeStatus(status);
        double longitudeDouble = (double)ByteArrHelper.fourbyte2int(longitude) / (double)1000000;
        double latitudeDouble = (double)ByteArrHelper.fourbyte2int(latitude) / (double)1000000;
        int heightInt = ByteArrHelper.twobyte2int(height);
        double speedDouble = (double) (ByteArrHelper.twobyte2int(speed)) / (double) 10;
        int directionInt = ByteArrHelper.twobyte2int(direction);
        String datetimeString = Jt808Helper.getDataTime(datetime);
        List<LocationAttachInfo> attachInfoList = new ArrayList<>();
        Analyzer.analyzeAttache((id, data) -> {
            LocationAttachInfo attachInfo = new LocationAttachInfo();
            attachInfo.setId(id);
            attachInfo.setData(data);
            attachInfoList.add(attachInfo);
        }, attache);


        LocationInfo locationInfo = new LocationInfo();
        locationInfo.setAlarmInfo(alarmInfo);
        locationInfo.setStatusInfo(statusInfo);
        locationInfo.setLongitude(longitudeDouble);
        locationInfo.setLatitude(latitudeDouble);
        locationInfo.setHeight(heightInt);
        locationInfo.setSpeed(speedDouble);
        locationInfo.setDirection(directionInt);
        locationInfo.setDatetime(datetimeString);
        locationInfo.setAttachInfo(attachInfoList);
        return locationInfo;
    }

    /**
     * 分析驾驶员上报信息 兼容 2011 版本协议和 2013 版本协议
     * @param data 上报数据
     * @return 驾驶员信息
     */
    public static DriverInfo analyzeDriver(byte[] data){
        // 版本信息
        int ver = 0;

        if(data[0] == Const.NUMBER_1 || data[0] == Const.NUMBER_2 ){
            if( data[Const.NUMBER_7] == Const.NUMBER_0 ) {
                // 这里需要通过解析消息长度判断版本
                int len2011 = 0, len2013 = 0;
                try{
                    len2011 = 62 + data[0] + data[data[0] + 61];
                }catch(IndexOutOfBoundsException e){
                    log.warn(e.getMessage());
                }
                try{
                    len2013 = 34 + data[8] + data[data[8] + 29];
                }catch(IndexOutOfBoundsException e){
                    log.warn(e.getMessage());
                }

                if(data.length == len2011){
                    ver = Const.YEAR_2011;
                }
                if(data.length == len2013){
                    ver = Const.YEAR_2013;
                }
            }else if(data[Const.NUMBER_7] == Const.NUMBER_1
                    || data[Const.NUMBER_7] == Const.NUMBER_2
                    || data[Const.NUMBER_7] == Const.NUMBER_3
                    || data[Const.NUMBER_7] == Const.NUMBER_4){
                if(data.length == Const.NUMBER_8){
                    ver = Const.YEAR_2013;
                }else{
                    ver = Const.YEAR_2011;
                }
            } else {
                ver = Const.YEAR_2011;
            }
        } else {
            ver = Const.YEAR_2011;
        }

        if( ver == Const.YEAR_2011 ){
            return analyzeDriver2011(data);
        } else if (ver == Const.YEAR_2013){
            return analyzeDriver2013(data);
        }
        return null;
    }

    private static DriverInfo analyzeDriver2011(byte[] data){
        // 姓名长度
        int nameLength = data[0];
        // 驾驶员姓名
        byte[] name = ByteArrHelper.subByte(data,1,nameLength + 1);
        // 身份证编码
        byte[] idCard = ByteArrHelper.subByte(data,nameLength + 1,nameLength + 21);
        // 从业资格证编码
        byte[] certificate = ByteArrHelper.subByte(data,nameLength + 21,nameLength + 61);
        // 从业资格证发证机构名称长度 最后全是 所以不需要
//        int certificatePublishAgentNameLength = data[nameLength + 61];
        // 从业资格证发证机构名称
        byte[] certificatePublishAgentName = ByteArrHelper.subByte(data,nameLength + 62);

        DriverInfo driverInfo = new DriverInfo();

        try {
            driverInfo.setDriverName(Jt808Helper.toGB18030String(name));
            driverInfo.setIdCardNumber(Jt808Helper.toGB18030String(idCard));
            driverInfo.setCertificateNumber(Jt808Helper.toGB18030String(certificate));
            driverInfo.setCertificatePublishAgentName(Jt808Helper.toGB18030String(certificatePublishAgentName));
        } catch (UnsupportedEncodingException e) {
             log.warn(e.getMessage());
        }

        driverInfo.setSuccess(true);

        return driverInfo;
    }

    private static DriverInfo analyzeDriver2013(byte[] data){
        DriverInfo driverInfo = new DriverInfo();
        driverInfo.setDriverAlarmInfo(new DriverAlarmInfo());

        if(data[0] == Const.NUMBER_1){
            // 驾驶员上班 卡插入
            driverInfo.getDriverAlarmInfo().setPullOutCard(false);
        }else if(data[0] == Const.NUMBER_2){
            // 驾驶员下班 卡拔出
            driverInfo.getDriverAlarmInfo().setPullOutCard(true);
        }else{
            // 2013 第一个字节 不支持其他形式
            return null;
        }
        String gTime = Jt808Helper.getDataTime(ByteArrHelper.subByte(data,1,7));

        driverInfo.setDatetime(gTime);

        switch (data[7]){
            case 0x00:
                // IC 卡读卡成功 读取驾驶员信息
                try {
                    int nameLength = data[8];
                    String driverName = Jt808Helper.toGB18030String(ByteArrHelper.subByte(data,9,
                            nameLength + 9));
                    String certificate = Jt808Helper.toGB18030String(ByteArrHelper.subByte(data,nameLength + 9,
                            nameLength + 29));
                    int certificatePublishAgentNameLength = data[nameLength + 29];
                    String certificatePublishAgentName =
                            Jt808Helper.toGB18030String(ByteArrHelper.subByte(data,nameLength + 30,
                                    nameLength + 30 + certificatePublishAgentNameLength));
                    String expiryTime = ByteArrHelper.getBCDStrByArr(ByteArrHelper.subByte(data,
                            nameLength + 30 + certificatePublishAgentNameLength));

                    driverInfo.setDriverName(driverName);
                    driverInfo.setCertificateNumber(certificate);
                    driverInfo.setCertificatePublishAgentName(certificatePublishAgentName);
                    driverInfo.setCertificateLimitDate(expiryTime);

                } catch (UnsupportedEncodingException e) {
                    log.warn(e.getMessage());
                    return null;
                }
                break;
            case 0x01:
                // 读卡失败，原因为卡片密钥认证未通过
                driverInfo.getDriverAlarmInfo().setUnAuthentication(true);
                break;
            case 0x02:
                // 读卡失败，原因为卡片已被锁定
                driverInfo.getDriverAlarmInfo().setLocked(true);
                break;
            case 0x03:
                // 读卡失败，原因为卡片被拔出
                driverInfo.getDriverAlarmInfo().setPullOut(true);
                break;
            case 0x04:
                // 读卡失败，原因为数据校验错误
                driverInfo.getDriverAlarmInfo().setCheckFailed(true);
                break;
            default:
                return null;
        }

        driverInfo.setSuccess(true);

        return driverInfo;
    }

    /**
     * 分析CAN总线上传数据
     * @param data 相关消息体
     */
    public static CanDataInfo analyzeCan(byte[] data) {

        CanDataInfo canDataInfo = new CanDataInfo();

        canDataInfo.setTimestamp(System.currentTimeMillis());

        String time = ByteArrHelper.getBCDStr(new byte[]{data[2]}) + "-" +
                      ByteArrHelper.getBCDStr(new byte[]{data[3]}) + "-" +
                      ByteArrHelper.getBCDStr(new byte[]{data[4]}) + "-" +
                      ByteArrHelper.getBCDStr(new byte[]{data[5]}) +
                      ByteArrHelper.getBCDStr(new byte[]{data[6]});

        canDataInfo.setReceiveTime(time);
        canDataInfo.setData(ByteArrHelper.subByte(data, 7));
        return canDataInfo;
    }

    /**
     * 分析CAN总线数据项
     * @param data 参总线项目列表
     */
    public static void analyzeCanItem(CanDataItemProcessor processor, byte[] data) {
        for(int pos = 0, len = 12;pos < data.length; pos += len){
            byte[] head = ByteArrHelper.subByte(data, pos,pos + 4);
            byte[] tail = ByteArrHelper.subByte(data, pos + 4,pos + len);
            CanDataItem item = new CanDataItem();

            int headInt = ByteArrHelper.fourbyte2int(head);

            item.setCanTunnel(headInt & 0x80000000 >> 31);
            item.setFrameType(headInt & 0x40000000 >> 30);
            item.setDataCollectModel(headInt & 0x20000000 >> 29);
            item.setCanID(headInt & 0x1fffffff);
            item.setData(tail);

            processor.process(item);
        }
    }

    /**
     * 分析多媒体数据信息
     * @param data 多媒体信息
     * @return 分析结果
     */
    public static MediaInfo analyzeMediaInfo(byte[] data) {
        MediaInfo mediaInfo = new MediaInfo();
        mediaInfo.setMediaId(ByteArrHelper.fourbyte2int(ByteArrHelper.subByte(data,0,4)));
        mediaInfo.setMediaType(data[4]);
        mediaInfo.setMediaFormat(data[5]);
        mediaInfo.setEventNumber(data[6]);
        mediaInfo.setTunnelId(data[7]);
        return mediaInfo;
    }

    /**
     * 数据透传分析
     * @param data 消息体
     * @return 分析结果
     */
    public static DataTransportInfo analyzeDataTransport(byte[] data) {
        DataTransportInfo dataTransportInfo = new DataTransportInfo();
        dataTransportInfo.setType(data[0]);
        dataTransportInfo.setData(ByteArrHelper.subByte(data, 1));
        return dataTransportInfo;
    }
}
