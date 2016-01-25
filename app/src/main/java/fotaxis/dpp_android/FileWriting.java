package fotaxis.dpp_android;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created by awais on 22.10.15.
 */
public class FileWriting {

    File mpiFile;
    FileOutputStream fOut;
    OutputStreamWriter myOutWriter;
    String lineSeparator;

    //FileOutputStream []fos;
    OutputStreamWriter osw;

    public FileWriting() {
        //fos=new FileOutputStream[5];

        lineSeparator=System.getProperty("line.separator");
        openFiles();
    }

    public void openFiles(){
        try {

            if(isExternalStorageWritable()){
                mpiFile = new File(Environment.getExternalStorageDirectory()+"/dpp/file/");
            }else{
                Log.e("mpi", "External Storage isn't writable");
            }


            mpiFile.mkdirs();

            File tempFile=new File(mpiFile,"data.txt");
            if(!tempFile.exists()){
                tempFile.createNewFile();
            }

            osw =new OutputStreamWriter(new FileOutputStream(tempFile,true));

        } catch (IOException e) {
            e.printStackTrace();
            closeFile();
        }

    }

    private void write(String txt,boolean fl_false){
        try {
            if(osw!=null){
                osw.append(txt);
                osw.append(lineSeparator);
                if(fl_false){
                    osw.flush();
                }
            }else{
                //something wrong with permissions!?
            }

        } catch (IOException e) {
            e.printStackTrace();
            closeFile();
        }

    }

    public void closeFile(){
        try{
            if(osw!=null){
                osw.close();
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }


    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


}
