package com.darshan09200.carrentingcenter;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Toast;

import com.darshan09200.carrentingcenter.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ArrayList<String> carNames = Database.getInstance().getCarNames();
        ArrayAdapter<String> carNamesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, carNames);
        carNamesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.carName.setAdapter(carNamesAdapter);

        binding.carName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Car currentCar = Database.getInstance().getCar(position);
                    if (currentCar != null) {
                        binding.dailyRent.setText(String.format("$ %.2f", currentCar.getDailyRent()));
                    }
                } else {
                    binding.dailyRent.setText("");
                }
                onSubmit(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                onSubmit(false);
            }
        });

        binding.noOfDaysSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.noOfDays.setText(String.format("%d Day%s", progress, progress > 1 ? "s" : ""));
                onSubmit(false);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        binding.ageGroup.setOnCheckedChangeListener((group, checkedId) -> onSubmit(false));
        binding.gps.setOnCheckedChangeListener((buttonView, isChecked) -> onSubmit(false));
        binding.childSeat.setOnCheckedChangeListener((buttonView, isChecked) -> onSubmit(false));
        binding.unlimitedMileage.setOnCheckedChangeListener((buttonView, isChecked) -> onSubmit(false));

        binding.viewDetails.setOnClickListener(v -> onSubmit(true));

    }

    private void onSubmit(boolean validate) {
        Car carDetails = getCarDetails();
        if (carDetails == null) {
            if (validate) showToast("Please Choose a Car");
            binding.amountLabel.setText("Amount");
            binding.totalPaymentLabel.setText("Total Payment");
            return;
        }
        int noOfDays = binding.noOfDaysSeekbar.getProgress();
        AgeGroup ageGroup = getSelectedAgeGroup();
        boolean isGpsChecked = binding.gps.isChecked();
        boolean isChildSeatChecked = binding.childSeat.isChecked();
        boolean isUnlimitedMileageChecked = binding.unlimitedMileage.isChecked();

        double amountBeforeTaxes = 0;
        double dailyAmount = carDetails.getDailyRent();

        if (ageGroup == AgeGroup.UNDER_20) dailyAmount += Database.UNDER_20_CHARGES;
        if (isGpsChecked) dailyAmount += Database.GPS_CHARGES;
        if (isChildSeatChecked) dailyAmount += Database.CHILD_SEAT_CHARGES;
        if (isUnlimitedMileageChecked) dailyAmount += Database.UNLIMITED_MILEAGE_CHARGES;

        System.out.println(isGpsChecked + " " + isChildSeatChecked + " " + isUnlimitedMileageChecked);
        amountBeforeTaxes = dailyAmount * noOfDays;

        double discount = 0;
        if (ageGroup == AgeGroup.ABOVE_60) discount = Database.ABOVE_60_DISCOUNT;

        amountBeforeTaxes -= discount;
        double taxes = amountBeforeTaxes * 0.13;
        double amountAfterTaxes = amountBeforeTaxes + taxes;

        binding.amountLabel.setText(String.format("Amount: %.2f", amountBeforeTaxes));
        binding.totalPaymentLabel.setText(String.format("Total Payment: %.2f", amountAfterTaxes));

        if(validate) {
            Intent intent = new Intent(this, DetailsActivity.class);

            intent.putExtra("carName", carDetails.getName());
            intent.putExtra("dailyRent", carDetails.getDailyRent());
            intent.putExtra("noOfDays", noOfDays);
            intent.putExtra("ageGroup", ageGroup);
            intent.putExtra("isGpsChecked", isGpsChecked);
            intent.putExtra("isChildSeatChecked", isChildSeatChecked);
            intent.putExtra("isUnlimitedMileageChecked", isUnlimitedMileageChecked);
            intent.putExtra("discount", discount);
            intent.putExtra("amount", amountBeforeTaxes);
            intent.putExtra("taxes", taxes);
            intent.putExtra("totalPayment", amountAfterTaxes);

            startActivity(intent);
        }
    }

    private Car getCarDetails() {
        return Database.getInstance().getCar(binding.carName.getSelectedItemPosition());
    }

    private AgeGroup getSelectedAgeGroup() {
        switch (binding.ageGroup.getCheckedRadioButtonId()) {
            case R.id.lessThanAge:
                return AgeGroup.UNDER_20;
            case R.id.aboveAge:
                return AgeGroup.ABOVE_60;
            default:
                return AgeGroup.BETWEEN_21_AND_60;
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if(Database.getInstance().shouldClearData()){
            binding.carName.setSelection(0);
            binding.noOfDaysSeekbar.setProgress(1);
            binding.ageGroup.check(R.id.lessThanAge);
            binding.gps.setChecked(false);
            binding.childSeat.setChecked(false);
            binding.unlimitedMileage.setChecked(false);
        }

        Database.getInstance().resetClearDataFlag();
    }
}