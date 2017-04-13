package pt.jigsawsolver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class SolverActivity extends Activity {

    ImageButton cameraButton;
    ImageButton galleryButton;
    ImageButton saveButtonUnsolved;
    ImageButton saveButtonSolved;

    ImageView photoView;
    SolverSurfaceView livePreview;
    SurfaceHolder holder;
    private Camera.PictureCallback mPicture;

    Camera camera;

    private static int PICK_IMAGE = 1;
    private static int CAM_REQUEST = 2;

    Boolean cameraStopped = false;
    Uri uriSavedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solver);

        cameraButton = (ImageButton) findViewById(R.id.cameraButtonSolver);
        galleryButton = (ImageButton) findViewById(R.id.galleryButtonSolver);
        saveButtonUnsolved = (ImageButton) findViewById(R.id.saveImgButtonUnsolved);
        saveButtonSolved = (ImageButton) findViewById(R.id.saveImgButtonSolved);

        photoView = (ImageView) findViewById(R.id.photoViewSolver);
        livePreview = (SolverSurfaceView) findViewById(R.id.livePreviewSolver);
        holder = livePreview.getHolder();

        //Camera
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
              // Canvas canvas = holder.lockCanvas();

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }

        });

          mPicture = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                try {
                    Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);

                    FileOutputStream outStream = null;
                    String filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/savedimg.jpeg";
                    File f = new File(filepath);
                    outStream = new FileOutputStream(f);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                    outStream.flush();
                    outStream.close();
                   // photoView.setImageBitmap(bmp);
                    Toast.makeText(getApplicationContext(), R.string.image_saved, Toast.LENGTH_LONG).show();

                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
                }
            }
        };

        //Turn on camera
        cameraButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (camera == null){
                    camera = Camera.open();
                }
                try {
                    camera.setPreviewDisplay(holder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                 camera.startPreview();
                cameraStopped = false;
            }
        });

        //Handle camera photo
        livePreview.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                if (camera != null) {
                    if (!cameraStopped) {
                        camera.stopPreview();
                        camera.takePicture(null, null, mPicture);
                    } else {
                        camera.startPreview();
                    }
                    cameraStopped = !cameraStopped;
                }
            }
        });

        //Take photo from gallery
        galleryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("image/*");

                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/*");

                Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

                startActivityForResult(chooserIntent, PICK_IMAGE);
            }
        });

        //Save taken photo
        saveButtonUnsolved.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Bitmap bitmap;
                livePreview.setDrawingCacheEnabled(true);
                bitmap = Bitmap.createBitmap(livePreview.getDrawingCache());
                livePreview.setDrawingCacheEnabled(false);

                // Write File to internal Storage
                try {
                    FileOutputStream outStream = null;
                    String filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/unslovedImage.jpeg";
                    File f = new File(filepath);
                    outStream = new FileOutputStream(f);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                    outStream.flush();
                    outStream.close();
                    Toast.makeText(getApplicationContext(), R.string.image_saved, Toast.LENGTH_LONG).show();

                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });

        //Save solved image
        saveButtonSolved.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                try {
                    BitmapDrawable bd = (BitmapDrawable) photoView.getDrawable();
                    Bitmap bm = bd.getBitmap();

                    FileOutputStream outStream = null;
                    String filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/savedimg.jpeg";
                    File f = new File(filepath);
                    outStream = new FileOutputStream(f);
                    bm.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                    outStream.flush();
                    outStream.close();
                    Toast.makeText(getApplicationContext(), R.string.image_saved, Toast.LENGTH_LONG).show();

                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Image from galery
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            try {
                Bitmap pictureBitmap = getBitmapFromUri(data.getData());

                Mat picture = new Mat();
                Utils.bitmapToMat(pictureBitmap, picture);

                livePreview.setImage(pictureBitmap);

            } catch (Exception e){
                Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == CAM_REQUEST && resultCode == Activity.RESULT_OK) {

            try {
                Bitmap pictureBitmap = getBitmapFromUri(uriSavedImage);

                Mat picture = new Mat();
                Utils.bitmapToMat(pictureBitmap, picture);

              //  photoView.setImageBitmap(pictureBitmap);

            } catch (Exception e){
                Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
            }
        }
    }


    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = pfd.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        pfd.close();
        return image;
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        // Release the Camera because we don't need it when paused
        // and other activities might need to use it.
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

}
