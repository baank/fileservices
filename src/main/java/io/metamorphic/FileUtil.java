package io.metamorphic;

import io.metamorphic.fileservices.FileService;
import io.metamorphic.fileservices.FileServiceImpl;
import io.metamorphic.models.DatasetInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Properties;

public class FileUtil {

    private static final Logger log = LogManager.getLogger(FileUtil.class);

    public static void main(String[] args) {
        FileService svc = new FileServiceImpl();
        try {
            log.debug("arg0 - datasource name: " + args[0]);
            log.debug("arg1 - dataset name: " + args[1]);
            log.debug("arg2 - input file: " + args[2]);
            log.debug("arg3 - gen: " + args[3]);
            String gen = args[3].toLowerCase();
            String data = FileServiceImpl.readFileAsString(new File(args[2]));
            Properties props = new Properties();
            try {
                InputStream propsFile = FileUtil.class.getResourceAsStream("/.env");
                props.load(propsFile);
            } catch (FileNotFoundException e) {
                System.out.println("Does the '.env' file exist in the system path?");
                System.exit(1);
            } catch (IOException e) {
                System.out.println("Error loading properties. Is the '.env' file in the right format?");
                System.exit(1);
            }
            String br = new String(new char[80]).replace("\0", "-");
            System.out.println(br);
            System.out.print(data);
            System.out.println(br);
            DatasetInfo info = svc.extractMetadata(args[0], args[1], data);
            if ("curate".equals(gen)) {
                System.out.println(info.toCurationProperties(props));
            } else if ("ingest".equals(gen)) {
                System.out.println(info.toIngestionProperties(props));
            } else if ("file".equals(gen)) {
                System.out.println(info.toFileProperties());
            } else {
                System.out.print(info.toDDL());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
