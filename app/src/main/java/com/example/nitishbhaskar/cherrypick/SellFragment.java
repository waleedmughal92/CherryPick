package com.example.nitishbhaskar.cherrypick;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.Manifest;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import mehdi.sakout.fancybuttons.FancyButton;


/**
 * A simple {@link Fragment} subclass.
 */
public class SellFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final int REQUEST_TAKE_PHOTO = 100;
    File photoFile;
    TextInputEditText productName;
    TextInputEditText productDescription;
    TextInputEditText productPrice;
    TextInputEditText productDate;
    Calendar myCalendar;
    TextInputEditText myLocation;
    FancyButton submit;
    DatePickerDialog.OnDateSetListener date;
    ProductData sellProduct;
    Location location;
    HashMap product;
    INavigate navigationListener;

    public SellFragment() {
        // Required empty public constructor
    }

    public static SellFragment newInstance(int sectionNumber) {
        SellFragment sellFragment = new SellFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        sellFragment.setArguments(args);
        return sellFragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_sell, container, false);
        try {
            navigationListener = (INavigate) view.getContext();
        } catch (ClassCastException e) {
            throw new ClassCastException("Implementation missed out.");
        }
        productName = (TextInputEditText) view.findViewById(R.id.pName);
        productDescription = (TextInputEditText) view.findViewById(R.id.pDescription);
        productPrice = (TextInputEditText) view.findViewById(R.id.pPrice);
        productDate = (TextInputEditText) view.findViewById(R.id.pDate);
        myLocation = (TextInputEditText) view.findViewById(R.id.pLocation);
        myCalendar = Calendar.getInstance();
        submit = (FancyButton) view.findViewById(R.id.submitButton);
        sellProduct = new ProductData();
        date = new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                // TODO Auto-generated method stub
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateLabel();
            }

        };

        productDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(getContext(), date, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        myLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                String provider = locationManager.getBestProvider(criteria, true);
                // Getting Current Location
                if (ActivityCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                location = locationManager.getLastKnownLocation(provider);
                updateLocation(location);
            }
        });


        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkProductName() && checkProductDescription() && checkProductPrice() && checkProductDate()) {
                    product = new HashMap();
                    product.put("productName", productName.getText().toString());
                    product.put("description", productDescription.getText().toString());
                    product.put("datePostedOn", productDate.getText().toString());
                    product.put("price", productPrice.getText().toString());
                    product.put("location", "" + location.getLatitude() + ", " + location.getLongitude());
                    product.put("productId", productName.getText().toString().replace(" ", "") + "_" + productDate.getText().toString() + "_" + getTime());
                    Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePhotoIntent.resolveActivity(getContext().getPackageManager()) != null) {
                        // Create the File where the photo should go
                        photoFile = null;
                        try {
                            photoFile = createImageFile();
                        } catch (IOException ex) {
                            // Error occurred while creating the File
                        }
                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                    Uri.fromFile(photoFile));
                            startActivityForResult(takePhotoIntent, REQUEST_TAKE_PHOTO);
                        }
                    }
                    Toast.makeText(getContext(),"Product Successfully added",Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Please fill out all the fields given above", Toast.LENGTH_SHORT).show();
                }
            }
        });


        return view;
    }

    private String getTime(){
        Calendar c = Calendar.getInstance();
        System.out.println("Current time => " + c.getTime());

        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        String formattedDate = df.format(c.getTime());
        return formattedDate;
    }

    private void updateLabel() {

        String myFormat = "MM-dd-yy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        productDate.setText(sdf.format(myCalendar.getTime()));

    }

    private void updateLocation(Location location) {

        Double lat = location.getLatitude();
        Double lgt = location.getLongitude();
        String locationText = getAddress(lat, lgt);
        myLocation.setText(locationText);
    }

    public String getAddress(double latitude, double longitude) {
        String placeAddress = "";
        if (latitude != 0 && longitude != 0) {
            try {
                Geocoder geocoder = new Geocoder(getContext());
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                String address = addresses.get(0).getAddressLine(0);
                String city = addresses.get(0).getAddressLine(1);
                String country = addresses.get(0).getAddressLine(2);
                placeAddress = address + ", " + city + ", " + country;
                Log.d("TAG", "address = " + address + ", city = " + city + ", country = " + country);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getContext(), "latitude and longitude are null", Toast.LENGTH_LONG).show();
        }
        return placeAddress;
    }

    private boolean checkProductName(){
        if( productName.getText().toString().trim().equals("")) {
            productName.setError("Product name is required!");
            return false;
        }
        return true;
    }

    private boolean checkProductDescription(){
        if( productDescription.getText().toString().trim().equals("")) {
            productDescription.setError("Product Description is required!");
            return false;
        }
        return true;
    }

    private boolean checkProductPrice(){
        if( productPrice.getText().toString().trim().equals("") ) {
            productPrice.setError("Product price is required!");
            return false;
        }
        return true;
    }

    private boolean checkProductDate(){
        if( productDate.getText().toString().trim().equals("") ) {
            productDate.setError("Product price is required!");
            return false;
        }
        return true;
    }

    private boolean userLocation(){
        if( myLocation.getText().toString().trim().equals("") ) {
            myLocation.setError("Product location is required!");
            return false;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode != 100) {
                try {
                    CloudinaryCloud.upload(photoFile, product);
                    navigationListener.navigateToHome();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (resultCode == 100) {
                // User cancelled the image capture
                //finish();
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(DateT);
        String imageFileName = "capturedImage";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        return image;

    }



}