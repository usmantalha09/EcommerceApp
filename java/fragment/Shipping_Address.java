package com.themescoder.androidecommerce.fragment;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.places.api.Places;
import com.google.android.material.snackbar.Snackbar;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.material.tabs.TabLayout;
import com.themescoder.androidecommerce.Maps.PlacesFieldSelector;
import com.themescoder.androidecommerce.adapters.ShippingTimeSlotsAdapter;
import com.themescoder.androidecommerce.constant.ConstantValues;
import com.themescoder.androidecommerce.customs.DialogLoader;

import com.themescoder.androidecommerce.activities.MainActivity;
import com.themescoder.androidecommerce.R;

import java.util.ArrayList;
import java.util.List;

import com.themescoder.androidecommerce.app.App;
import com.themescoder.androidecommerce.models.address_model.AddressData;
import com.themescoder.androidecommerce.models.address_model.AddressDetails;
import com.themescoder.androidecommerce.models.address_model.Countries;
import com.themescoder.androidecommerce.models.address_model.CountryDetails;
import com.themescoder.androidecommerce.models.address_model.ZoneDetails;
import com.themescoder.androidecommerce.models.address_model.Zones;
import com.themescoder.androidecommerce.models.shipping_model.TimeSlotsList;
import com.themescoder.androidecommerce.network.APIClient;
import com.themescoder.androidecommerce.utils.Utilities;
import com.themescoder.androidecommerce.utils.ValidateInputs;

import am.appwise.components.ni.NoInternetDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;



import static android.app.Activity.RESULT_OK;


public class Shipping_Address extends Fragment implements GoogleApiClient.OnConnectionFailedListener {

    View rootView;
    Boolean isUpdate = false;
    String customerID, defaultAddressID, customerPhone;
    int selectedZoneID, selectedCountryID;

    List<String> zoneNames;
    List<String> countryNames;
    List<ZoneDetails> zoneList;
    List<CountryDetails> countryList;

    ArrayAdapter<String> zoneAdapter;
    ArrayAdapter<String> countryAdapter;

    Button proceed_checkout_btn;
    LinearLayout default_shipping_layout;

    EditText input_firstname, input_lastname, input_location, input_address, input_country, input_zone,
            delivery_time, input_city, input_postcode, input_phone;

    DialogLoader dialogLoader;

    //LocationPicker.
    /*Edited  28-Dec-18*/
    int PLACE_PICKER_REQUEST = 1463;
    String latitude, longitude;
    SharedPreferences pref;

    long deliveryTimeInMillis = 0;
    static long serverTime;

    private int year;
    private int month;
    private int day;

    private List<TimeSlotsList> timeSlotsLists;
    ShippingTimeSlotsAdapter shippingTimeSlotsAdapter;
    RecyclerView timeSlots;
    static String deliveryTimeSlot;
    static String deliveryCost;
    //DatePicker date_picker;
    int FLAG_DAY = 0;
    My_Cart my_cart;

    ViewPager viewPager;
    TabLayout tabLayout;

    PlacesFieldSelector fieldSelector;

    public Shipping_Address(My_Cart my_cart) {
        this.my_cart = my_cart;
    }


    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.address, container, false);

        NoInternetDialog noInternetDialog = new NoInternetDialog.Builder(getContext()).build();
        // noInternetDialog.show();

        if (getArguments() != null) {
            if (getArguments().containsKey("isUpdate")) {
                isUpdate = getArguments().getBoolean("isUpdate", false);
            }
        }


        /*Edited  28-Dec-18*/
        if (!Places.isInitialized()) {
            Places.initialize(getContext(), getString(R.string.place_picker_id));
        }
        fieldSelector = new PlacesFieldSelector();

        /*Edited 28-Dec-18*/


        // Set the Title of Toolbar
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.shipping_address));

        // Get the customersID and defaultAddressID from SharedPreferences

        pref = getContext().getSharedPreferences("UserInfo", getContext().MODE_PRIVATE);

        customerID = pref.getString("userID", "");
        defaultAddressID = pref.getString("userDefaultAddressID", "");
        customerPhone = pref.getString("userTelephone", "");


        // Binding Layout Views
        input_firstname = (EditText) rootView.findViewById(R.id.firstname);
        input_lastname = (EditText) rootView.findViewById(R.id.lastname);
        input_location = (EditText) rootView.findViewById(R.id.location);
        input_address = (EditText) rootView.findViewById(R.id.address);
        input_country = (EditText) rootView.findViewById(R.id.country);
        input_zone = (EditText) rootView.findViewById(R.id.zone);
        input_city = (EditText) rootView.findViewById(R.id.city);
        delivery_time = (EditText) rootView.findViewById(R.id.delivery_time);
        input_postcode = (EditText) rootView.findViewById(R.id.postcode);
        input_phone = (EditText) rootView.findViewById(R.id.contact);
        default_shipping_layout = (LinearLayout) rootView.findViewById(R.id.default_shipping_layout);
        proceed_checkout_btn = (Button) rootView.findViewById(R.id.save_address_btn);
        // Set KeyListener of some View to null
        input_country.setKeyListener(null);
        input_zone.setKeyListener(null);

        zoneNames = new ArrayList<>();
        countryNames = new ArrayList<>();

        // Hide the Default Checkbox Layout
        default_shipping_layout.setVisibility(View.GONE);

        delivery_time.setVisibility(View.GONE);
        input_location.setVisibility(View.GONE);

        // Set the text of Button
        proceed_checkout_btn.setText(getContext().getString(R.string.next));

        dialogLoader = new DialogLoader(getContext());

        // Request Countries
        RequestCountries();

        // If an existing Address is being Edited
        if (isUpdate) {
            // Get the Shipping AddressDetails from AppContext that is being Edited
            AddressDetails shippingAddress = ((App) getContext().getApplicationContext()).getShippingAddress();

            // Set the Address details
            selectedZoneID = shippingAddress.getZoneId();
            selectedCountryID = shippingAddress.getCountriesId();
            input_firstname.setText(shippingAddress.getFirstname());
            input_lastname.setText(shippingAddress.getLastname());
            input_address.setText(shippingAddress.getStreet());
            input_country.setText(shippingAddress.getCountryName());
            input_zone.setText(shippingAddress.getZoneName());
            input_city.setText(shippingAddress.getCity());
            input_postcode.setText(shippingAddress.getPostcode());
            input_phone.setText(shippingAddress.getPhone());

            RequestZones(String.valueOf(selectedCountryID));
        } else {
            // Request All Addresses of the User
            RequestAllAddresses();
            input_phone.setText(customerPhone);
            //input_phone.setEnabled(false);
        }

        /*Edited  28-Dec-18*/
        // Handle Touch event of input_location EditText
        /*
        input_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent autocompleteIntent = new Intent(getContext(), MapActivity.class);
                //requestCode in INT
                startActivityForResult(autocompleteIntent, PLACE_PICKER_REQUEST);
            }
        });
        */
        /*Edited  28-Dec-18*/
        // Handle Touch event of input_country EditText

        input_country.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP) {

                    countryAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1);
                    countryAdapter.addAll(countryNames);

                    AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
                    View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_list_search, null);
                    dialog.setView(dialogView);
                    dialog.setCancelable(false);

                    Button dialog_button = (Button) dialogView.findViewById(R.id.dialog_button);
                    EditText dialog_input = (EditText) dialogView.findViewById(R.id.dialog_input);
                    TextView dialog_title = (TextView) dialogView.findViewById(R.id.dialog_title);
                    ListView dialog_list = (ListView) dialogView.findViewById(R.id.dialog_list);

                    dialog_title.setText(getString(R.string.country));
                    dialog_list.setVerticalScrollBarEnabled(true);
                    dialog_list.setAdapter(countryAdapter);

                    dialog_input.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                            countryAdapter.getFilter().filter(charSequence);
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                        }
                    });


                    final AlertDialog alertDialog = dialog.create();

                    dialog_button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            alertDialog.dismiss();
                        }
                    });

                    alertDialog.show();


                    dialog_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                            alertDialog.dismiss();
                            final String selectedItem = countryAdapter.getItem(position);

                            int countryID = 0;
                            input_country.setText(selectedItem);

                            if (!selectedItem.equalsIgnoreCase("Other")) {

                                for (int i = 0; i < countryList.size(); i++) {
                                    if (countryList.get(i).getCountriesName().equalsIgnoreCase(selectedItem)) {
                                        // Get the ID of selected Country
                                        countryID = countryList.get(i).getCountriesId();
                                    }
                                }

                            }

                            selectedCountryID = countryID;

                            input_zone.setText("");

                            // Request for all Zones in the selected Country
                            RequestZones(String.valueOf(selectedCountryID));
                        }
                    });

                }

                return false;
            }
        });


        // Handle Touch event of input_zone EditText
        input_zone.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP) {

                    zoneAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1);
                    zoneAdapter.addAll(zoneNames);

                    AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
                    View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_list_search, null);
                    dialog.setView(dialogView);
                    dialog.setCancelable(false);

                    Button dialog_button = (Button) dialogView.findViewById(R.id.dialog_button);
                    EditText dialog_input = (EditText) dialogView.findViewById(R.id.dialog_input);
                    TextView dialog_title = (TextView) dialogView.findViewById(R.id.dialog_title);
                    ListView dialog_list = (ListView) dialogView.findViewById(R.id.dialog_list);

                    dialog_title.setText(getString(R.string.zone));
                    dialog_list.setVerticalScrollBarEnabled(true);
                    dialog_list.setAdapter(zoneAdapter);

                    dialog_input.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                            zoneAdapter.getFilter().filter(charSequence);
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                        }
                    });


                    final AlertDialog alertDialog = dialog.create();

                    dialog_button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            alertDialog.dismiss();
                        }
                    });

                    alertDialog.show();


                    dialog_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                            alertDialog.dismiss();
                            final String selectedItem = zoneAdapter.getItem(position);

                            int zoneID = 0;
                            input_zone.setText(selectedItem);

                            if (!zoneAdapter.getItem(position).equalsIgnoreCase("Other")) {

                                for (int i = 0; i < zoneList.size(); i++) {
                                    if (zoneList.get(i).getZoneName().equalsIgnoreCase(selectedItem)) {
                                        // Get the ID of selected Country
                                        zoneID = zoneList.get(i).getZoneId();
                                    }
                                }
                            }

                            selectedZoneID = zoneID;
                        }
                    });

                }

                return false;
            }
        });


        // Handle the Click event of Proceed Order Button
        proceed_checkout_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Validate Address Form Inputs
                boolean isValidData = validateAddressForm();

                if (isValidData) {
                    // New Instance of AddressDetails
                    AddressDetails shippingAddress = new AddressDetails();

                    shippingAddress.setFirstname(input_firstname.getText().toString().trim());
                    shippingAddress.setLastname(input_lastname.getText().toString().trim());
                    shippingAddress.setCountryName(input_country.getText().toString().trim());
                    shippingAddress.setZoneName(input_zone.getText().toString().trim());
                    shippingAddress.setCity(input_city.getText().toString().trim());
                    shippingAddress.setStreet(input_address.getText().toString().trim());
                    shippingAddress.setPostcode(input_postcode.getText().toString().trim());
                    shippingAddress.setPhone(input_phone.getText().toString().trim());
                    shippingAddress.setZoneId(selectedZoneID);
                    shippingAddress.setCountriesId(selectedCountryID);
                    //shippingAddress.setLatitude(latitude);
                    //shippingAddress.setLongitude(longitude);
                    //shippingAddress.setDelivery_time(delivery_time.getText().toString());
                    shippingAddress.setDelivery_cost(deliveryCost);
                    shippingAddress.setPacking_charge_tax("" + ConstantValues.PACKING_CHARGE);


                    // Save the AddressDetails
                    ((App) getContext().getApplicationContext()).setShippingAddress(shippingAddress);


                    // Check if an Address is being Edited
                    if (isUpdate) {
                        // Navigate to CheckoutFinal Fragment
                        ((MainActivity) getContext()).getSupportFragmentManager().popBackStack();
                    } else {
                        // Navigate to Billing_Address Fragment
                        Fragment fragment = new Billing_Address(my_cart);
                        FragmentManager fragmentManager = getFragmentManager();
                        fragmentManager.beginTransaction().add(R.id.main_fragment, fragment)
                                .addToBackStack(null).commit();
                    }
                }
            }
        });

        // Handle Touch event for delivery time
/*
        delivery_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!input_zone.getText().toString().isEmpty()) {

                    // Add RecentlyViewed Fragment to specified FrameLayout
                    showDateAndTimePicker();

                }

            }
        });
*/


        return rootView;
    }

    //*********** Get Countries List from the Server ********//

    private void RequestCountries() {

        Call<Countries> call = APIClient.getInstance()
                .getCountries();

        call.enqueue(new Callback<Countries>() {
            @Override
            public void onResponse(Call<Countries> call, Response<Countries> response) {

                if (response.isSuccessful()) {
                    if (response.body().getSuccess().equalsIgnoreCase("1")) {

                        countryList = response.body().getData();

                        // Add the Country Names to the countryNames List
                        for (int i = 0; i < countryList.size(); i++) {
                            countryNames.add(countryList.get(i).getCountriesName());
                        }

                        countryNames.add("Other");

                    } else if (response.body().getSuccess().equalsIgnoreCase("0")) {
                        Snackbar.make(rootView, response.body().getMessage(), Snackbar.LENGTH_LONG).show();

                    } else {
                        // Unable to get Success status
                        Snackbar.make(rootView, getString(R.string.unexpected_response), Snackbar.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Countries> call, Throwable t) {
                Toast.makeText(getContext(), "NetworkCallFailure : " + t, Toast.LENGTH_LONG).show();
            }
        });
    }


    //*********** Get Zones List of the Country from the Server ********//

    private void RequestZones(String countryID) {

        Call<Zones> call = APIClient.getInstance()
                .getZones
                        (
                                countryID
                        );

        call.enqueue(new Callback<Zones>() {
            @Override
            public void onResponse(Call<Zones> call, Response<Zones> response) {

                if (response.isSuccessful()) {

                    if (response.body().getSuccess().equalsIgnoreCase("1")) {

                        zoneNames.clear();
                        zoneList = response.body().getData();

                        // Add the Zone Names to the zoneNames List
                        for (int i = 0; i < zoneList.size(); i++) {
                            zoneNames.add(zoneList.get(i).getZoneName());
                        }

                        zoneNames.add("Other");

                    } else if (response.body().getSuccess().equalsIgnoreCase("0")) {
                        Snackbar.make(rootView, response.body().getMessage(), Snackbar.LENGTH_LONG).show();

                    } else {
                        // Unable to get Success status
                        Snackbar.make(rootView, getString(R.string.unexpected_response), Snackbar.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Zones> call, Throwable t) {
                Toast.makeText(getContext(), "NetworkCallFailure : " + t, Toast.LENGTH_LONG).show();
            }
        });
    }


    //*********** Get the User's Default Address from all the Addresses ********//

    private void filterDefaultAddress(AddressData addressData) {

        // Get CountryList from Response
        List<AddressDetails> addressesList = addressData.getData();

        // Initialize new AddressDetails for DefaultAddress
        AddressDetails defaultAddress = new AddressDetails();

        for (int i = 0; i < addressesList.size(); i++) {

            int id = addressesList.get(i).getAddressId();
            int address = addressesList.get(i).getDefaultAddress();
            // Check if the Current Address is User's Default Address
            if (addressesList.get(i).getDefaultAddress() == 1) {
                // Set the Default AddressDetails
                defaultAddress = addressesList.get(i);
            }
        }

        // Set Default Address Data and Display to User
        selectedZoneID = defaultAddress.getZoneId();
        selectedCountryID = defaultAddress.getCountriesId();
        input_firstname.setText(defaultAddress.getFirstname());
        input_lastname.setText(defaultAddress.getLastname());
        input_address.setText(defaultAddress.getStreet());
        input_country.setText(defaultAddress.getCountryName());
        input_zone.setText(defaultAddress.getZoneName());
        input_city.setText(defaultAddress.getCity());
        input_postcode.setText(defaultAddress.getPostcode());

        // Request Zones of selected Country
        RequestZones(String.valueOf(selectedCountryID));
    }


    //*********** Request List of User Addresses ********//

    public void RequestAllAddresses() {

        dialogLoader.showProgressDialog();

        Call<AddressData> call = APIClient.getInstance()
                .getAllAddress
                        (
                                customerID
                        );

        call.enqueue(new Callback<AddressData>() {
            @Override
            public void onResponse(Call<AddressData> call, retrofit2.Response<AddressData> response) {

                dialogLoader.hideProgressDialog();

                // Check if the Response is successful
                if (response.isSuccessful()) {
                    if (response.body().getSuccess().equalsIgnoreCase("1")) {

                        // Filter all the Addresses to get the Default Address
                        filterDefaultAddress(response.body());


                    }
                    else {

                        Snackbar.make(rootView,response.body().getMessage(),Snackbar.LENGTH_SHORT).show();
                    }

                }
                else {
                    Toast.makeText(getContext(),response.message(),Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AddressData> call, Throwable t) {
                dialogLoader.hideProgressDialog();
                Toast.makeText(getContext(), "NetworkCallFailure : " + t, Toast.LENGTH_LONG).show();
            }
        });
    }

    //*********** Validate Address Form Inputs ********//

    private boolean validateAddressForm() {
        if (!ValidateInputs.isValidName(input_firstname.getText().toString().trim())) {
            input_firstname.setError(getString(R.string.invalid_first_name));
            return false;
        } else if (!ValidateInputs.isValidName(input_lastname.getText().toString().trim())) {
            input_lastname.setError(getString(R.string.invalid_last_name));
            return false;
        } else if (input_address.getText().toString().trim().isEmpty()) {
            input_address.setError(getString(R.string.invalid_address));
            return false;
        } else if (!ValidateInputs.isValidInput(input_country.getText().toString().trim())) {
            input_country.setError(getString(R.string.select_country));
            return false;
        } else if (!ValidateInputs.isValidInput(input_zone.getText().toString().trim())) {
            input_zone.setError(getString(R.string.select_zone));
            return false;
        } else if (!ValidateInputs.isValidInput(input_city.getText().toString().trim())) {
            input_city.setError(getString(R.string.enter_city));
            return false;
        }

/*
        else if (delivery_time.getText().toString().trim().isEmpty()) {
            delivery_time.setError(getString(R.string.enter_delivery_time_date));
            return false;
        }
*/

        else if (!ValidateInputs.isValidNumber(input_postcode.getText().toString().trim())) {
            input_postcode.setError(getString(R.string.invalid_post_code));
            return false;
        } else if (!ValidateInputs.isValidPhoneNo(input_phone.getText().toString().trim())) {
            input_phone.setError(getString(R.string.invalid_contact));
            return false;
        }

/*
        else if (input_location.getText().toString().isEmpty()) {
            input_location.setError("Invalid Location Points");
            return false;
        }
*/

        else {
            return true;
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                String lat = data.getStringExtra("lat");
                String lon = data.getStringExtra("lon");
                latitude = lat;
                longitude = lon;
                String address = Utilities.getCompleteAddressString(getContext(),Double.parseDouble(latitude),Double.parseDouble(longitude));
                input_location.setText(latitude + ", " + longitude);
                input_address.setText(address);
//                 place.getName();
//                 place.getAddress();
            }
            else {
                Log.e("ShippingAddress","Error in data fetching");
               // Toast.makeText(getActivity(),   "Error in data fetching", Toast.LENGTH_SHORT).show();
            }



        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getActivity(), connectionResult.getErrorMessage() + "", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
//        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
     //   mGoogleApiClient.disconnect();
    }

    @Override
    public void onPause() {
      //  mGoogleApiClient.stopAutoManage(getActivity());
     //   mGoogleApiClient.disconnect();
        super.onPause();
    }
}

