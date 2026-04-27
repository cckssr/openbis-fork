/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.afssftp.server;

import ch.ethz.sis.afssftp.authentication.OpenBISPasswordAuthenticator;
import ch.ethz.sis.afssftp.filesystemview.OpenBISFileSystemFactory;
import ch.ethz.sis.afssftp.startup.AfsSftpServerParameter;
import ch.ethz.sis.afssftp.util.OpenBISClientUtil;
import ch.ethz.sis.shared.log.standard.LogFactory;
import ch.ethz.sis.shared.log.standard.LogFactoryFactory;
import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import ch.ethz.sis.shared.startup.Configuration;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.netty.NettyIoServiceFactoryFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Collections;

public final class Server {

    private final Logger logger;
    private final SshServer sftpServer;
    private volatile boolean shutdown;

    public Server(Configuration configuration) throws Exception
    {
        // Load logging plugin, Initializing LogManager
        shutdown = false;

        LogFactoryFactory logFactoryFactory = new LogFactoryFactory();
        LogFactory logFactory = logFactoryFactory.create(configuration.getStringProperty(AfsSftpServerParameter.logFactoryClass));
        logFactory.configure(configuration.getStringProperty(AfsSftpServerParameter.logConfigFile));
        LogManager.setLogFactory(logFactory);
        logger = LogManager.getLogger(Server.class);

        configuration.logLoadedProperties(LogManager.getLogger(Configuration.class));

        logger.info("=== Server Bootstrap ===");
        logger.info("Running with java.version: " + System.getProperty("java.version"));

        // Startup
        OpenBISClientUtil.applicationServerUrl.set(
                configuration.getStringProperty(AfsSftpServerParameter.applicationServerUrl)
        );
        OpenBISClientUtil.afsUrl.set(
                configuration.getStringProperty(AfsSftpServerParameter.afsUrl)
        );
        sftpServer = SshServer.setUpDefaultServer();

        // 1. Mandatory: Force the server to use Netty instead of default NIO2
        sftpServer.setIoServiceFactoryFactory(new NettyIoServiceFactoryFactory());

        // 2. Setup SFTP subsystem
        sftpServer.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        sftpServer.setFileSystemFactory(new OpenBISFileSystemFactory());

        // 3. Configure port and start
        int serverPort = configuration.getIntegerProperty(AfsSftpServerParameter.serverPort);
        sftpServer.setPort(serverPort);

        // 4. Set key pair provider
        Path ksPath = Paths.get(configuration.getStringProperty(AfsSftpServerParameter.keyStorePath));
        String type = "JKS";
        String password = configuration.getStringProperty(AfsSftpServerParameter.keyStorePassword);
        String alias = configuration.getStringProperty(AfsSftpServerParameter.keyStoreKeyAlias);

        KeyStore ks = loadKeyStore(ksPath.toString(), password, type);
        KeyPair kp = loadKeyPair(ks, alias, password);

        sftpServer.setKeyPairProvider(KeyPairProvider.wrap(kp));

        // 5. Set Authenticator
        sftpServer.setPasswordAuthenticator(new OpenBISPasswordAuthenticator());
        //sshd.setPasswordAuthenticator(AcceptAllPasswordAuthenticator.INSTANCE); //possibly useful for local tests

        sftpServer.start();
        System.out.printf("SFTP server started on port %s%n", serverPort);

        createServerStartedFile();
        logger.info("=== Server ready ===");
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            public void run()
            {
                try
                {
                    shutdown();
                } catch (Exception e)
                {
                    logger.catching(e);
                }
            }
        });
    }

    private void createServerStartedFile()
    {
        File STARTED_FILE = new File("SERVER_STARTED");
        try
        {
            STARTED_FILE.createNewFile();
            STARTED_FILE.deleteOnExit();
            logger.info(STARTED_FILE.getAbsolutePath()+" created");
        } catch (IOException ex)
        {
            logger.catching(new RuntimeException("Couldn't create marker file " + STARTED_FILE, ex));
        }
    }

    public void shutdown() throws Exception
    {
        if (!shutdown) {
            sftpServer.stop();
            shutdown = true;
            logger.info("Shutting down");
        }
    }

    public static KeyStore loadKeyStore(String ksPath, String password, String type) throws Exception {
        // If type is null, default to PKCS12 (standard for modern Java) or JKS
        if (type == null) type = KeyStore.getDefaultType();

        KeyStore ks = KeyStore.getInstance(type);
        try (FileInputStream fis = new FileInputStream(ksPath)) {
            ks.load(fis, password.toCharArray());
        }
        return ks;
    }

    public static KeyPair loadKeyPair(KeyStore ks, String alias, String password) throws Exception {
        // 1. Get the Private Key
        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, password.toCharArray());

        // 2. Get the Public Key from the Certificate
        Certificate cert = ks.getCertificate(alias);
        if (cert == null) {
            throw new Exception("No certificate found for alias: " + alias);
        }
        PublicKey publicKey = cert.getPublicKey();

        return new KeyPair(publicKey, privateKey);
    }
}
