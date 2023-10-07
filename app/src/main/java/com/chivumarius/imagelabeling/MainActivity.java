package com.chivumarius.imagelabeling;



import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

import android.util.Log;
import android.database.Cursor;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;


public class MainActivity extends AppCompatActivity {

    // ▼ "DECLARATION" OF "VIEWS" → FROM "UI" ▼
    ImageView innerImage;
    TextView resultTv;
    CardView cardImages,cardCamera;

    private Uri image_uri;


    // ▼ "DECLARATION" OF "IMAGE LABELER" → FROM "IMAGE LABELING" LIBRARY ▼
    ImageLabeler labeler;




    // ▼ "GALERY ACTIVITY RESULT LAUNCHER"
    //      → TO GET THE "IMAGE" FROM "GALLERY"
    //      → AND DISPLAY IT ▼
    ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        image_uri = result.getData().getData();
                        Bitmap inputImage = uriToBitmap(image_uri);
                        Bitmap rotated = rotateBitmap(inputImage);
                        innerImage.setImageBitmap(rotated);
                        performImageLabeling(rotated);
                    }
                }
            });



    // ▼ "CAMERA ACTIVITY RESULT LAUNCHER"
    //      → TO "CAPTURE" THE "IMAGE"
    //      → USING "CAMERA" AND "DISPLAY IT" ▼
    ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Bitmap inputImage = uriToBitmap(image_uri);
                        Bitmap rotated = rotateBitmap(inputImage);
                        innerImage.setImageBitmap(rotated);
                        performImageLabeling(rotated);
                    }
                }
            });




    //TODO declare image labeler


    // ▬▬ "ON CREATE()" METHOD ▬▬
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ▼ "INITIALIZATION" OF "INNER IMAGE" VIEW → FROM "UI" ▼
        innerImage = findViewById(R.id.imageView2);

        // ▼ SETTING THE "SIZE PROGRAMMATICALLY" OF "INNER IMAGE" ("RESPONSIVE") ▼
        ViewGroup.LayoutParams layoutParams = innerImage.getLayoutParams();
        layoutParams.height = getWidth() - 40;
        innerImage.setLayoutParams(layoutParams);


        // ▼ "INITIALIZATION" OF "VIEW" → FROM "UI" ▼
        resultTv = findViewById(R.id.textView);
        cardImages = findViewById(R.id.cardImages);
        cardCamera = findViewById(R.id.cardCamera);




        // ▼ SET "ON CLICK LISTENER" → ON "CARD IMAGES"
        //      → TO "CHOOSE IMAGES" FROM "GALLERY" ▼
        cardImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ▼ "LAUNCHING" THE "GALLERY" SO THAT "USER" CAN "SELECT" AN "IMAGE" ▼
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryActivityResultLauncher.launch(galleryIntent);
            }
        });




        // ▼ SET "ON CLICK LISTENER" → ON "CARD CAMERA"
        //      → TO "CAPTURE IMAGES" USING "CAMERA" ▼
        cardCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    // ▼ "CHECKING": IF "CAMERA PERMISSION" IS "GRANTED" OR "NOT" ▼
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED){
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permission, 1211);
                    }
                    else {
                        // ▼ "CALLING" THE "METHOD" ▼
                        openCamera();
                    }

                }  else {
                    // ▼ "CALLING" THE "METHOD" ▼
                    openCamera();
                }
            }
        });



        // ▼ "INITIALIZATION" OF "IMAGE LABELER" → FROM "IMAGE LABELING" LIBRARY ▼
        //      → TO "USE" "DEFAULT OPTIONS" ▼
//        labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);



        // ▼ "SETTING" THE "MINIMUM CONFIDENCE REQUIRED" FOR "IMAGE LABELING"
        //      → TO "0.7"
        //      → TO "SHOW" ONLY "2 PREDICTIONS" ▼
        ImageLabelerOptions options =
             new ImageLabelerOptions.Builder()
                 .setConfidenceThreshold(0.7f)
                 .build();

        labeler = ImageLabeling.getClient(options);
}





    // ▬ "OPEN CAMERA()" METHOD ▬
    //TODO TO "OPEN" THE "CAMERA" SO THAT "USER" CAN "CAPTURE IMAGES" ▼
    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        cameraActivityResultLauncher.launch(cameraIntent);
    }




    // ▬ "PERFORM IMAGE LABELING()" METHOD
    //      → TO PERFORM "IMAGE LABELING" ON "IMAGES" ▬
    public void performImageLabeling(Bitmap input){
        // ▼ CREATING AN "INPUT IMAGE" OBJECT ▼
        InputImage image = InputImage.fromBitmap(input, 0);

        // ▼ RE-SETTING THE "RESULT" ▼
        resultTv.setText("");


        // ▼ PERFORMING "IMAGE LABELING" ▼
        labeler.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {

                    // ▬ "ON SUCCESS" METHOD ▬
                    @Override
                    public void onSuccess(List<ImageLabel> labels) {

                        // ▼ "LOOPING" THROUGH EACH "IMAGE LABEL" OBJECT
                        //      → FROM THE "LABELS" LIST ▼
                        for(ImageLabel label : labels){
                            // CREATING EACH "IMAGE LABEL" OBJECT ▼
                            //resultTv.append(label.getText() + " " + label.getConfidence() + "\n");

                            // ▼ "SHOWING" ONLY "2 FRAGMENTS DIGITS" ▼
                            resultTv.append(label.getText() + " "
                                    + String.format("%.2f", label.getConfidence()) + "\n");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {

                    // ▬ "ON FAILURE" METHOD ▬
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                    }
                });

    }




    // ▬ "ON REQUEST PERMISSIONS RESULT()" METHOD ▬
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            //TODO show live camera footage
            openCamera();
        } else {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
        }
    }






    // ▬ "ROTATE BITMAP()" METHOD
    //      → TO "ROTATE IMAGES" IF "IMAGE" IS "CAPTURED" ON "SAMSUNG DEVICES" ▬
    @SuppressLint("Range")
    public Bitmap rotateBitmap(Bitmap input){
        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
        Cursor cur = getContentResolver().query(image_uri, orientationColumn, null, null, null);
        int orientation = -1;
        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
        }
        Log.d("tryOrientation",orientation+"");
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.setRotate(orientation);
        Bitmap cropped = Bitmap.createBitmap(input,0,0, input.getWidth(), input.getHeight(), rotationMatrix, true);
        return cropped;
    }




    // ▬ "URI TO BITMAP()" METHOD
    //      → IT "TAKES" A "URI" OF THE "IMAGE" AND RETURNS" A BITMAP" ▬
    private Bitmap uriToBitmap(Uri selectedFileUri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(selectedFileUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);

            parcelFileDescriptor.close();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  null;
    }



    // ▬ "GET WIDTH()" METHOD ▬
    public int getWidth(){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

}
