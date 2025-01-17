package com.dtstack.engine.alert.client.mail.impl;

import com.alibaba.fastjson.JSONObject;
import com.dtstack.engine.alert.AlterContext;
import com.dtstack.engine.alert.client.mail.AbstractMailAlterClient;
import com.dtstack.engine.alert.client.mail.AlterSendMailBean;
import com.dtstack.engine.alert.enums.AlertGateCode;
import com.dtstack.engine.alert.exception.AlterException;
import com.dtstack.lang.data.R;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * @Auther: dazhi
 * @Date: 2021/1/18 2:03 下午
 * @Email:dazhi@dtstack.com
 * @Description:
 */
public class DTMailAlterClient extends AbstractMailAlterClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected R sendMail(AlterSendMailBean alterSendMailBean) {
        long startTime = System.currentTimeMillis();
        try {
            HtmlEmail htmlEmail = new HtmlEmail();
            htmlEmail.setHostName(alterSendMailBean.getHost());
            htmlEmail.setSmtpPort(alterSendMailBean.getPort());
            htmlEmail.setAuthentication(alterSendMailBean.getUsername(), alterSendMailBean.getPassword());
            htmlEmail.setSSLOnConnect(alterSendMailBean.getSsl());
            htmlEmail.setCharset("UTF-8");

            htmlEmail.setFrom(alterSendMailBean.getFrom());
            htmlEmail.addTo(alterSendMailBean.getEmails().toArray(new String[]{}));
            htmlEmail.setSubject(alterSendMailBean.getSubject());
            htmlEmail.setHtmlMsg(alterSendMailBean.getContent());

            List<File> attachFiles = alterSendMailBean.getAttachFiles();
            if (attachFiles != null) {
                attachFiles.forEach(x -> {
                    try {
                        htmlEmail.attach(x);
                    } catch (EmailException e) {
                        logger.error("加载文件失败！");
                    }
                });
            }

            String result = htmlEmail.send();
            logger.info("[sendMail] end, cost={}, mails={}, subject={}, message={}, result={}", (System.currentTimeMillis() - startTime), alterSendMailBean.getEmails(), alterSendMailBean.getSubject(),
                    alterSendMailBean.getContent(), result);
            return R.ok();
        } catch (EmailException e) {
            logger.info("[sendMail] end, cost={}, mails={}, subject={}, message={}", (System.currentTimeMillis() - startTime), alterSendMailBean.getEmails(), alterSendMailBean.getSubject(),
                    alterSendMailBean.getContent(), e);
            return R.fail(e.getLocalizedMessage());
        }

    }

    @Override
    public void buildBean(AlterSendMailBean alterSendMailBean, JSONObject jsonObject, AlterContext alterContext) throws AlterException {
        // DT 必传项
        String host = jsonObject.getString(ConstMailAlter.MAIL_HOST);
        if (StringUtils.isBlank(host)) {
            throw new AlterException("发送邮件必须有邮箱服务器地址，请在配置中配置字段:"+ ConstMailAlter.MAIL_HOST);
        }

        Integer port = jsonObject.getInteger(ConstMailAlter.MAIL_PORT);
        if (port == null) {
            throw new AlterException("发送邮件必须有邮箱服务器端口，请在配置中配置字段:"+ ConstMailAlter.MAIL_PORT);
        }

        String username = jsonObject.getString(ConstMailAlter.MAIL_USERNAME);
        if (StringUtils.isBlank(username)) {
            throw new AlterException("发送邮件必须有邮箱发送者账号，请在配置中配置字段:"+ ConstMailAlter.MAIL_USERNAME);
        }

        String password = jsonObject.getString(ConstMailAlter.MAIL_PASSWORD);
        if (StringUtils.isBlank(password)) {
            throw new AlterException("发送邮件必须有邮箱发送者，请在配置中配置字段:"+ ConstMailAlter.MAIL_USERNAME);
        }

        String from = jsonObject.getString(ConstMailAlter.MAIL_FROM);
        if (StringUtils.isBlank(from)) {
            from = username;
        }

        Boolean ssl = jsonObject.getBoolean(ConstMailAlter.MAIL_SSL);
        if (ssl == null) {
            ssl= Boolean.FALSE;
        }

        alterSendMailBean.setHost(host);
        alterSendMailBean.setPort(port);
        alterSendMailBean.setUsername(username);
        alterSendMailBean.setPassword(password);
        alterSendMailBean.setFrom(from);
        alterSendMailBean.setSsl(ssl);
    }

    @Override
    protected String getAlertGateCode() {
        return AlertGateCode.AG_GATE_MAIL_DT.code();
    }
}
