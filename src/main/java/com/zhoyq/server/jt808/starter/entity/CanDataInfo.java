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

package com.zhoyq.server.jt808.starter.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * @author zhoyq <a href="mailto:feedback@zhoyq.com">feedback@zhoyq.com</a>
 * @date 2020/2/20
 */
@Setter
@Getter
public class CanDataInfo {

    /**
     * 数据接受时间戳
     */
    private long timestamp;

    /**
     * 数据接收时间 hh-mm-ss-msms
     */
    private String receiveTime;

    /**
     * CAN总线数据项
     */
    private byte[] data;

}
