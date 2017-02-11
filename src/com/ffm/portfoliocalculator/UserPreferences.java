package com.ffm.portfoliocalculator;

import android.os.Bundle;
import android.text.method.KeyListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.AdapterView.OnItemSelectedListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

public class UserPreferences extends Activity {
	
	private Spinner taxBracketSpinner;
	private EditText incomeTaxRate;
	private EditText capitalGainsRate;
	private SharedPreferences savedPreferences;
	private KeyListener incTaxListener; //used to lock/unlock the editText field
	private KeyListener capGainsListener; //used to lock/unlock the editText field
	
	public void onCreate(Bundle savedInstanceState) 
	   {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.user_preferences);
			
			savedPreferences = getSharedPreferences("preferences", MODE_PRIVATE);
			
			taxBracketSpinner = (Spinner) findViewById(R.id.taxBracketSpinner);
			
			incomeTaxRate = (EditText) findViewById(R.id.editIncomeTax);
			capitalGainsRate = (EditText) findViewById(R.id.editCapitalGains);
			
			incTaxListener = incomeTaxRate.getKeyListener();
			capGainsListener = capitalGainsRate.getKeyListener();
			
			getActionBar().hide();
			
			//Initializes the tax bracket spinner
			ArrayAdapter<CharSequence> taxBracketSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.incomeSegement_array, android.R.layout.simple_spinner_item);
			taxBracketSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			taxBracketSpinner.setAdapter(taxBracketSpinnerAdapter);
			taxBracketSpinner.setOnItemSelectedListener(spinnerListener);
			int spinnerSelection = savedPreferences.getInt("tax bracket", 3);
			taxBracketSpinner.setSelection(spinnerSelection, true);
			
	   } 
		
		//Selects the tax rates to display depending on the user's selected tax bracket
		private OnItemSelectedListener spinnerListener = new OnItemSelectedListener(){
			
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				boolean editable = false; //determines whether the income tax and capital gains tax fields can be edited
				int selectedTaxBracket = taxBracketSpinner.getLastVisiblePosition();
				String selectedIncomeTaxRate;
				String selectedCapitalGainsRate;
				switch (selectedTaxBracket) {
					case 0: 
					{
						selectedIncomeTaxRate = "0";
						selectedCapitalGainsRate = "0";
						break;
					}
					case 1: 
					{
						selectedIncomeTaxRate = "10";
						selectedCapitalGainsRate = "0";
						break;
					}
					case 2: 
					{
						selectedIncomeTaxRate = "15";
						selectedCapitalGainsRate = "0";
						break;
					}
					case 3: 
					{
						selectedIncomeTaxRate = "25";
						selectedCapitalGainsRate = "15";
						break;
					}
					case 4: 
					{
						selectedIncomeTaxRate = "28";
						selectedCapitalGainsRate = "15";
						break;
					}
					case 5: 
					{
						selectedIncomeTaxRate = "33";
						selectedCapitalGainsRate = "15";
						break;
					}
					case 6: 
					{
						selectedIncomeTaxRate = "35";
						selectedCapitalGainsRate = "15";
						break;
					}
					case 7: 
					{
						selectedIncomeTaxRate = "39.6";
						selectedCapitalGainsRate = "20";
						break;
					}
					default: //for manually adjusted tax rate
					{
						selectedIncomeTaxRate = savedPreferences.getString("income tax rate", "0");
						selectedCapitalGainsRate = savedPreferences.getString("capital gains rate", "0");
						editable = true;
						break;
					}
				}
				
					//sets values of EditTexts
					incomeTaxRate.setText(selectedIncomeTaxRate);
					capitalGainsRate.setText(selectedCapitalGainsRate);
					
					//locks or unlocks the fields depending on whether manually adjust is selected
					if (!editable) {
						incomeTaxRate.setKeyListener(null);
						capitalGainsRate.setKeyListener(null);
					} else {
						incomeTaxRate.setKeyListener(incTaxListener);
						capitalGainsRate.setKeyListener(capGainsListener);
						incomeTaxRate.requestFocus();
					}
				
			}
		
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		};

		//Ensures the user has entered a valid tax rate (i.e. not blank nor greater than 100)
		private boolean validateNumbers() {
			String incomeTaxField = incomeTaxRate.getText().toString();
			String capitalGainsField = capitalGainsRate.getText().toString();
			if(incomeTaxField.equals("") || capitalGainsField.equals("")) {
				String message = "Please enter a valid tax rate";
				createDialogBox(message);
				return false;
			} else if (Double.parseDouble(incomeTaxField)>100 || Double.parseDouble(capitalGainsField)>100) {
				String message = "Please choose a number less than 100%";
				createDialogBox(message);
				return false;
			}
			return true;
		}
		
		//Creates dialog box with provided @param message
		private void createDialogBox(String message) {
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			alertDialogBuilder.setTitle("WARNING");
			alertDialogBuilder.setMessage(message);
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		}
		
		@Override
		public void onBackPressed() {
			if (validateNumbers()) {
				saveDialogBox();
			}
		}
		
		//saves the user preferences to memory
		private void savePreferences() {
			SharedPreferences.Editor preferencesEditor = savedPreferences.edit();
			preferencesEditor.putInt("tax bracket", taxBracketSpinner.getLastVisiblePosition());
			preferencesEditor.putString("income tax rate", incomeTaxRate.getText().toString());
			preferencesEditor.putString("capital gains rate", capitalGainsRate.getText().toString());
			preferencesEditor.apply();
		}
		
		//Save Dialog Box created when the user presses the back button. If the user presses yes, then the PortfolioViewer is created again
		private void saveDialogBox() {
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(UserPreferences.this);
			alertDialogBuilder.setTitle("Save Preferences?");
			alertDialogBuilder.setMessage("Do you want to save these changes?");
			alertDialogBuilder.setCancelable(true);
			alertDialogBuilder.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,int id) {
					savePreferences();
                    Intent sendData = new Intent (UserPreferences.this, PortfolioViewer.class);
                    startActivity(sendData);
				}
			});
			alertDialogBuilder.setNegativeButton("No",new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,int id) {
					finish();
				}
			});
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		}
}
