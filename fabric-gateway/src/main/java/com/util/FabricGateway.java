package com.util;

import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

//@PropertySource("classpath:/application.yml")
@Slf4j
@Configuration
public class FabricGateway {
    @Value("${fabric.walletDirectory}")
    String walletDirectory;
    @Value("${fabric.certificatePath}")
    String certificatePath;
    @Value("${fabric.username}")
    String username;
    @Value("${fabric.networkConfigPath}")
    String networkConfigPath;
    @Value("${fabric.privateKeyPath}")
    String privateKeyPath;
    @Value("${fabric.mspid}")
    String mspid;
    @Value("${fabric.channelName}")
    String channelName;
    @Value("${fabric.usercontract}")
    String contractName;
    @Value("${fabric.cercontract}")
    String cercontract;

    /**
     * 连接网关
     */
    @Bean
    public Gateway connectGateway() throws IOException, InvalidKeyException, CertificateException {
        //使用org1中的user1初始化一个网关wallet账户用于连接网络
        Wallet wallet = Wallets.newFileSystemWallet(Paths.get(this.walletDirectory));
        X509Certificate certificate = readX509Certificate(Paths.get(this.certificatePath));
        PrivateKey privateKey = getPrivateKey(Paths.get(this.privateKeyPath));
        wallet.put(username, Identities.newX509Identity(this.mspid, certificate, privateKey));

        //根据connection.json 获取Fabric网络连接对象
        Gateway.Builder builder = Gateway.createBuilder()
                .identity(wallet, username)
                .networkConfig(Paths.get(this.networkConfigPath));


        //连接网关
        return builder.connect();
    }

    /**
     * 获取通道
     */
    @Bean
    public Network network(Gateway gateway) {
        return gateway.getNetwork(this.channelName);
    }

    /**
     * 获取合约
     */
    @Bean
    public Contract contract(Network network) {
        return network.getContract(this.contractName);
    }

//    @Bean
//    public Contract cercontract(Network network) {
//        return network.getContract(this.cercontract);
//    }

    private static X509Certificate readX509Certificate(final Path certificatePath) throws IOException, CertificateException {
        try (Reader certificateReader = Files.newBufferedReader(certificatePath, StandardCharsets.UTF_8)) {
            return Identities.readX509Certificate(certificateReader);
        }
    }

    private static PrivateKey getPrivateKey(final Path privateKeyPath) throws IOException, InvalidKeyException {
        try (Reader privateKeyReader = Files.newBufferedReader(privateKeyPath, StandardCharsets.UTF_8)) {
            return Identities.readPrivateKey(privateKeyReader);
        }
    }
}
