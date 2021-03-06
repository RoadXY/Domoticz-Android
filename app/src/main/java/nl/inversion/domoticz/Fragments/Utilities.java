package nl.inversion.domoticz.Fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.widget.ListView;

import java.util.ArrayList;

import nl.inversion.domoticz.Adapters.UtilityAdapter;
import nl.inversion.domoticz.Containers.UtilitiesInfo;
import nl.inversion.domoticz.Domoticz.Domoticz;
import nl.inversion.domoticz.Interfaces.DomoticzFragmentListener;
import nl.inversion.domoticz.Interfaces.UtilitiesReceiver;
import nl.inversion.domoticz.Interfaces.setCommandReceiver;
import nl.inversion.domoticz.Interfaces.thermostatClickListener;
import nl.inversion.domoticz.R;
import nl.inversion.domoticz.app.DomoticzFragment;

public class Utilities extends DomoticzFragment implements DomoticzFragmentListener,
        thermostatClickListener {

    private Domoticz mDomoticz;
    private ArrayList<UtilitiesInfo> mUtilitiesInfos;


    private long thermostatSetPointValue;

    private ListView listView;
    private UtilityAdapter adapter;
    private ProgressDialog progressDialog;
    private Activity mActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        getActionBar().setTitle(R.string.title_utilities);
    }

    @Override
    public void onConnectionOk() {
        showProgressDialog();

        final thermostatClickListener listener = this;

        mDomoticz = new Domoticz(mActivity);
        mDomoticz.getUtilities(new UtilitiesReceiver() {

            @Override
            public void onReceiveUtilities(ArrayList<UtilitiesInfo> mUtilitiesInfos) {
                successHandling(mUtilitiesInfos.toString(), false);

                Utilities.this.mUtilitiesInfos = mUtilitiesInfos;

                adapter = new UtilityAdapter(mActivity, mUtilitiesInfos, listener);
                listView = (ListView) getView().findViewById(R.id.listView);
                listView.setAdapter(adapter);

                hideProgressDialog();
            }

            @Override
            public void onError(Exception error) {
                errorHandling(error);
            }
        });
    }

    @Override
    public void onClick(final int idx, int action, long newSetPoint) {
        addDebugText("onClick");
        addDebugText("Set idx " + idx + " to " + String.valueOf(newSetPoint));

        thermostatSetPointValue = newSetPoint;

        int jsonUrl = Domoticz.Json.Url.Set.TEMP;

        mDomoticz.setAction(idx, jsonUrl, action, newSetPoint, new setCommandReceiver() {
            @Override
            public void onReceiveResult(String result) {
                updateThermostatSetPointValue(idx, thermostatSetPointValue);
                successHandling(result, false);
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
        addDebugText("updateThermostatSetPointValue");

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
        addDebugText("notifyDataSetChanged");
        // adapter.notifyDataSetChanged();
        listView.setAdapter(adapter);

    }

    /**
     * Initializes the progress dialog
     */
    private void initProgressDialog() {
        progressDialog = new ProgressDialog(this.getActivity());
        progressDialog.setMessage(getString(R.string.msg_please_wait));
        progressDialog.setCancelable(false);
    }

    /**
     * Shows the progress dialog if isn't already showing
     */
    private void showProgressDialog() {
        if (progressDialog == null) initProgressDialog();
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

    @Override
    public void errorHandling(Exception error) {
        super.errorHandling(error);
        hideProgressDialog();
    }
}