package org.dalnservice.classes;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.Result;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static net.sf.expectit.matcher.Matchers.regexp;

/**
 * Connects to EC2 instance via SSH that run DALN worker
 */
public class DALNSSHClient {

    static final Logger logger = Logger.getLogger(DALNSSHClient.class);

    private static final String PEM_LOCOTAION = "ec2_pem/lpattach383.pem";
    private static final String EC2_HOSTNAME = "52.70.237.216";
    private static final String EC_USER = "ubuntu";
    private DALNS3Client s3Client;

    private static DALNSSHClient instance;
    private static final String DEV_BUCKET = "daln-development-sb";
    private static final String PROD_BUCKET = "daln-prod-sb";
    private static final String DEV_SERVICE_NAME = "DALNUploadServiceDev";
    private static final String PROD_SERVICE_NAME = "DALNUploadServiceProd";


    public static DALNSSHClient getInstance() throws IOException{
        if (instance == null){
            instance = new DALNSSHClient();
        }
        return instance;
    }

    private DALNSSHClient() throws IOException {
        s3Client = DALNS3Client.getInstance();
    }

    /**
     * Connects to EC2 instance with DALN worker and run specified command
     * @param stage "prod" or "dev" - the stage to get the key
     * @param command the command to run(linux console)
     * @return completion status
     * @throws IOException
     */
    public String runCommand(String stage, String command) throws IOException{

        String result = "Failed";
        String bucket = stage.equals("prod") ? PROD_BUCKET : DEV_BUCKET;
        String serviceName = stage.equals("prod") ? PROD_SERVICE_NAME : DEV_SERVICE_NAME;
        logger.debug("Start restarting service "+serviceName);
        InputStream in = s3Client.downloadFile(bucket, PEM_LOCOTAION);

        final SSHClient ssh = new SSHClient();
        Session session = null;
        Expect expect = null;
        try {
            String privateKey = convertStreamToString(in);
            logger.debug("Start of key "+privateKey.substring(0,20));
            KeyProvider keyProvider = ssh.loadKeys(privateKey,null,null);
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect(EC2_HOSTNAME);
            ssh.authPublickey(EC_USER, keyProvider);
            session = ssh.startSession();
            logger.debug("Session started");
            Session.Shell shell = session.startShell();
            logger.debug("Shell started");
            expect = new ExpectBuilder()
                    .withOutput(shell.getOutputStream())
                    .withInputs(shell.getInputStream(), shell.getErrorStream())
                    .withExceptionOnFailure()
                    .build();
            logger.debug("Expect created");
            expect.expect(regexp("Welcome"));// wait greeting message on login
            // run script that will do everything
            expect.sendLine(command+"; echo qazwsxedc");
            Result expect1 = expect.expect(regexp("qazwsxedc"));
            String input = expect1.getInput();
            String[] split = input.split("\n");
            List<String> out = new ArrayList<>(Arrays.asList(split));
            out = out.subList(Math.max(0, out.size()-100), out.size()-1);
            String join = StringUtils.join(out, "\n");
            logger.debug(join);
            logger.debug("Done executing command");
            result = join;
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (IOException e) {
                // Do Nothing
            }
            if (expect != null){
                expect.close();
            }
            ssh.disconnect();
        }
        return result;
    }

    private String convertStreamToString(InputStream is) {
        java.util.Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
