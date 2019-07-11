package com.routz.fabric_java_sdk_integration.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.routz.fabric_java_sdk_integration.entity.Skr;
import com.routz.fabric_java_sdk_integration.mapper.SkrMapper;
import com.routz.fabric_java_sdk_integration.util.FabricUtils;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class SkrService extends ServiceImpl<SkrMapper, Skr> {
    private Logger logger = LoggerFactory.getLogger("Skr~");
    volatile private FabricUtils fu = new FabricUtils();
    @Autowired
    private SkrMapper skrMapper;
    @Autowired
    private DataSourceTransactionManager transactionManager;

    public SkrService() throws Exception {
    }

    public void checkin(String rfid, String readerId) throws Exception {
        String uuid = UUID.randomUUID().toString();
        // 判断重复时间，确保key不在同一个块上重复操作
        CompletableFuture<BlockEvent.TransactionEvent> checkin = fu.checkin(new Date().toString(), rfid, readerId);
        checkin.thenAcceptAsync(myEvent -> {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.execute(txStatus -> {
                    logger.warn("===" + myEvent.getTransactionID());
                Skr skr = new Skr();
                skr.setId(uuid);
                skr.setRfid(rfid);
                skr.setName(myEvent.getTransactionID());
                skrMapper.insert(skr);
                return null;
            });
        }).exceptionally(ex -> {
            if (ex instanceof TransactionEventException) {
                // 重试？
                ex.printStackTrace();
            }
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            log.warn(sw.toString());
            return null;
        });

    }
}
