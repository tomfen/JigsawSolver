package pt.jigsawsolver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class HintActivity extends Activity {

    ImageButton cameraButton;
    ImageButton galleryButton;
    ImageButton saveButton;

    ImageView photoView;
    SurfaceView livePreview;
    SurfaceHolder holder;

    Camera camera;
    private Camera.PictureCallback mPicture;
    private Camera.AutoFocusCallback mFocus;

    private static int PICK_IMAGE = 1;
    private static int CAM_REQUEST = 2;

    Boolean cameraStopped = false;
    Uri uriSavedImage;

    Mat element = null;
    Mat picture = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hint);

        cameraButton = (ImageButton) findViewById(R.id.cameraButton);
        galleryButton = (ImageButton) findViewById(R.id.galleryButton);
        saveButton = (ImageButton) findViewById(R.id.saveImgButton);

        photoView = (ImageView) findViewById(R.id.photoView);
        livePreview = (SurfaceView) findViewById(R.id.livePreview);

        holder = livePreview.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (camera == null) {
                    try {
                        camera = Camera.open();
                        Camera.Parameters params = camera.getParameters();
                        List<String> focus = params.getSupportedFocusModes();
                        if (focus.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                            camera.setParameters(params);
                        }
                        camera.setDisplayOrientation(90);
                        camera.setPreviewDisplay(holder);
                        camera.startPreview();
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), R.string.camera_error, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }

        });

        mPicture = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                if(picture == null) return;

                try {
                    Bitmap elementBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                    element = new Mat();
                    Utils.bitmapToMat(elementBitmap, element);

                    Mat result = JigsawFitter.find(element, picture);
                    if (result != null) {
                        Bitmap resultBmp = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.RGB_565);
                        Utils.matToBitmap(result, resultBmp);

                        photoView.setImageBitmap(resultBmp);
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.piece_not_found, Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
                }
            }
        };

        mFocus = new Camera.AutoFocusCallback() {

            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    camera.takePicture(null, null, mPicture);
                }
            }
        };

        livePreview.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (picture == null) {
                    Toast.makeText(getApplicationContext(), R.string.template_not_loaded, Toast.LENGTH_LONG).show();
                } else if (camera != null) {
                    if (!cameraStopped) {
                        camera.autoFocus(mFocus);
                    } else {
                        camera.startPreview();
                    }
                    cameraStopped = !cameraStopped;
                }
            }
        });

        cameraButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                String picturePath = Environment.getExternalStorageDirectory() + "/pic.jpg";

                uriSavedImage = Uri.fromFile(new File(picturePath));

                camera_intent.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImage);
                startActivityForResult(camera_intent, CAM_REQUEST);
            }
        });

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

        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                try {
                    BitmapDrawable bd = (BitmapDrawable) photoView.getDrawable();
                    Bitmap pictureBitmap = bd.getBitmap();

                    FileOutputStream outStream = null;
                    String filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/savedimg.jpeg";
                    File f = new File(filepath);
                    outStream = new FileOutputStream(f);
                    pictureBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
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
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        // Release the Camera because we don't need it when paused
        // and other activities might need to use it.
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            try {
                Bitmap pictureBitmap = getBitmapFromUri(data.getData());

                picture = new Mat();
                Utils.bitmapToMat(pictureBitmap, picture);

                photoView.setImageBitmap(pictureBitmap);

            } catch (Exception e){
                Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == CAM_REQUEST && resultCode == Activity.RESULT_OK) {

            try {
                Bitmap pictureBitmap = getBitmapFromUri(uriSavedImage);

                picture = new Mat();
                Utils.bitmapToMat(pictureBitmap, picture);

                photoView.setImageBitmap(pictureBitmap);

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

}