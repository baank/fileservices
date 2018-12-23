package io.metamorphic;

import io.metamorphic.fileservices.FileService;
import io.metamorphic.fileservices.FileServiceImpl;
import io.metamorphic.models.DatasetInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class FileUtil {

    private static final Logger log = LogManager.getLogger(FileUtil.class);

    public static void main(String[] args) {
        FileService svc = new FileServiceImpl();
        try {
            log.debug("arg0 - dataset name: " + args[0]);
            log.debug("arg1 - input file: " + args[1]);
            String data = FileServiceImpl.readFileAsString(new File(args[1]));
            String br = new String(new char[80]).replace("\0", "-");
            System.out.println(br);
            System.out.print(data);
            System.out.println(br);
            DatasetInfo info = svc.extractMetadata(args[0], data);
            System.out.print(info.toDDL());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
