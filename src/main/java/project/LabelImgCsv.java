package project;


import org.apache.commons.io.FilenameUtils;
import org.datavec.image.recordreader.objdetect.ImageObject;
import org.datavec.image.recordreader.objdetect.ImageObjectLabelProvider;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;


public class LabelImgCsv implements ImageObjectLabelProvider{

    private Map<String, List<ImageObject>> labelMap = new HashMap();

    // Constructor
    public LabelImgCsv(File dir, String csvPath) throws IOException{
        File[] listOfFiles = dir.listFiles();
        Scanner csvscan = new Scanner(new File(csvPath));
        String line = csvscan.nextLine();
        String[] lineArray = line.split(",");

        for (int i = 0; i < listOfFiles.length; i++) {
            String filename = listOfFiles[i].getName();

            System.out.println("filename[" + i + "] = " + filename);
            System.out.println("lineArray[0] = " + lineArray[0]);
            int j = 0;
            ArrayList<ImageObject> list = new ArrayList();
            while (filename.equals(lineArray[0])) {
                String strj = String.valueOf(j++);
                String name = "character " + strj;
                int xmin = Integer.parseInt(lineArray[1]);
                int ymin = Integer.parseInt(lineArray[2]);
                int xmax = Integer.parseInt(lineArray[3]);
                int ymax = Integer.parseInt(lineArray[4]);
                list.add(new ImageObject(xmin, ymin, xmax, ymax, name));

                if(!csvscan.hasNext()) {
                    break;
                }
                line = csvscan.nextLine();
                lineArray = line.split(",");
            }
            System.out.println("list : ");
            System.out.println(list);
            this.labelMap.put(FilenameUtils.getBaseName(filename), list);

        }

    }


    public List<ImageObject> getImageObjectsForPath(String path) {
        File file = new File(path);
        String filename = file.getName();
        return (List)this.labelMap.get(FilenameUtils.getBaseName(filename));
    }

    public List<ImageObject> getImageObjectsForPath(URI uri) {

        return this.getImageObjectsForPath(uri.toString());
    }
}



