package com.huangliang.api.entity.response;

import com.huangliang.api.constants.CommonConsts;

public class Message
{
    /**
     * ret = 0 时返回true
     * ret != 0 时返回false
     */
    private boolean success;
    
    /**
     * 消息状态码
     * 0 表示成功
     * 其它表示失败
     */
    private String ret;
    
    /**
     * 消息说明
     */
    private String retInfo;

    public Message()
    {
        this.ret = CommonConsts.ERROR;
    }

    public Message(String ret, String retInfo)
    {
        this.ret = ret;
        this.retInfo = retInfo;
        if(CommonConsts.SUCCESS.equals(ret))
        {
            success = true;
        }
        else
        {
            success = false;
        }
    }

}
