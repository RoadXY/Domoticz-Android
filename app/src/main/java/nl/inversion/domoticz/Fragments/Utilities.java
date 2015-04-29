package nl.inversion.domoticz.Fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import nl.inversion.domoticz.Adapters.UtilityAdapter;
import nl.inversion.domoticz.Containers.UtilitiesInfo;
import nl.inversion.domoticz.Domoticz.Domoticz;
import nl.inversion.domoticz.Interfaces.setCommandReceiver;
import nl.inversion.domoticz.Interfaces.UtilitiesReceiver;
import nl.inversion.domoticz.Interfaces.ThermostatButtonClickListener;
import nl.inversion.domoticz.R;

public class Utilities extends Fragment implements ThermostatButtonClickListener {

    private static final String TAG = Utilities.class.getSimpleName();

    private Domoticz mDomoticz;
    private ArrayList<UtilitiesInfo> mUtilitiesInfos;

    private int clickedIdx;
    private long thermostatSetPointValue;

    private ListView utilitiesListView;
    private UtilityAdapter adapter;
    private ProgressDialog progressDialog;
    private TextView debugText;
    private boolean debug;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_utilities, null);

        getActionBar().setTitle(R.string.title_utilities);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mDomoticz = new Domoticz(getActivity());
        debug = Domoticz.debug;


        // Initialize the progress dialog
        progressDialog = new ProgressDialog(this.getActivity());
        progressDialog.setMessage(getString(R.string.msg_please_wait));
        progressDialog.setCancelable(false);

        if (debug) {
            debugText = (TextView) getView().findViewById(R.id.debugText);
            debugText.setVisibility(View.VISIBLE);
        }


    }

    @Override
    public void onResume() {
        super.onResume();
        getData();
    }

    private void getData() {

        showProgressDialog();

        final ThermostatButtonClickListener listener = this;

        mDomoticz.getUtilities(new UtilitiesReceiver() {


            @Override
            public void onReceiveUtilities(ArrayList<UtilitiesInfo> mUtilitiesInfos) {
                successHandling(mUtilitiesInfos.toString());

                Utilities.this.mUtilitiesInfos = mUtilitiesInfos;

                adapter = new UtilityAdapter(getActivity(), mUtilitiesInfos, listener);
                utilitiesListView = (ListView) getView().findViewById(R.id.utilitiesListView);
                utilitiesListView.setAdapter(adapter);

                hideProgressDialog();
            }

            @Override
            public void onError(Exception error) {
                errorHandling(error);
            }
        });

    }

    @Override
    public void onThermostatClick(int idx, int action, long newSetPoint) {

        clickedIdx = idx;
        thermostatSetPointValue = newSetPoint;

        Log.d(TAG, "onThermostatClick");

        int jsonUrl = Domoticz.JSON_SET_URL_TEMP;
        int jsonAction = mDomoticz.JSON_ACTION_MIN;

        switch (action) {
            case Domoticz.THERMOSTAT_ACTION_MIN:
                Log.d(TAG, "Set idx " + idx + " to min");
                jsonAction = mDomoticz.JSON_ACTION_MIN;
                break;

            case Domoticz.THERMOSTAT_ACTION_PLUS:
                Log.d(TAG, "Set idx " + idx + " to plus");
                jsonAction = mDomoticz.JSON_ACTION_PLUS;
                break;

        }

        mDomoticz.setAction(idx, jsonUrl, jsonAction, newSetPoint, new setCommandReceiver() {
            @Override
            public void onReceiveResult(String result) {
                updateThermostatSetPointValue(clickedIdx, thermostatSetPointValue);
                Toast.makeText(getActivity(), R.string.action_success, Toast.LENGTH_SHORT).show();
                successHandling(result);
            }

            @Override
            public void onError(Exception error) {
                errorHandling(error);
            }
        });

    }

    /**
     * Updates the set point in the Utilities container
     * @param idx ID of the utility to be changed
     * @param newSetPoint The new set point value
     */
    private void updateThermostatSetPointValue(int idx, long newSetPoint) {

        for (UtilitiesInfo info : mUtilitiesInfos) {
            if (info.getIdx() == idx) {
                info.setSetPoint(newSetPoint);
                break;
            }
        }
        notifyDataSetChanged();

    }

    /**
     * Notifies the list view adapter the data has changed and refreshes the list view
     */
    private void notifyDataSetChanged() {

        adapter.notifyDataSetChanged();
        // utilitiesListView.setAdapter(adapter);

    }

    /**
     * Shows the progress dialog if isn't already showing
     */
    private void showProgressDialog() {
        if (!progressDialog.isShowing())
            progressDialog.show();
    }

    /**
     * Hides the progress dialog if it is showing
     */
    private void hideProgressDialog() {
        if (progressDialog.isShowing())
            progressDialog.dismiss();
    }

    /**
     * Handles the success messages
     *
     * @param result String result to handle
     */
    private void successHandling(String result) {
        hideProgressDialog();

        Log.d(TAG, result);
        if (debug) {
            String temp = debugText.getText().toString();
            debugText.setText(temp + "\n\n" + result);
        }
    }

    /**
     * Handles the error messages
     *
     * @param error Exception
     */
    private void errorHandling(Exception error) {
        hideProgressDialog();

        error.printStackTrace();

        if (debug) {
            String temp = debugText.getText().toString();
            debugText.setText(temp + "\n\n" + error.getCause().getMessage());
        } else {
            mDomoticz.errorToast(error);
        }
    }

    private ActionBar getActionBar() {
        return ((ActionBarActivity) getActivity()).getSupportActionBar();
    }
}