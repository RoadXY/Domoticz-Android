package nl.inversion.domoticz.Fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import nl.inversion.domoticz.Adapters.SwitchesAdapter;
import nl.inversion.domoticz.Containers.ExtendedStatusInfo;
import nl.inversion.domoticz.Containers.SwitchInfo;
import nl.inversion.domoticz.Domoticz.Domoticz;
import nl.inversion.domoticz.Interfaces.DomoticzFragmentListener;
import nl.inversion.domoticz.Interfaces.StatusReceiver;
import nl.inversion.domoticz.Interfaces.SwitchesReceiver;
import nl.inversion.domoticz.Interfaces.setCommandReceiver;
import nl.inversion.domoticz.Interfaces.switchesClickListener;
import nl.inversion.domoticz.R;
import nl.inversion.domoticz.UI.switchInfoDialog;
import nl.inversion.domoticz.app.DomoticzFragment;

public class Switches extends DomoticzFragment implements DomoticzFragmentListener,
        switchesClickListener,
        switchInfoDialog.InfoDialogSwitchChangeListener {

    private static final String TAG = Temperature.class.getSimpleName();
    private final ArrayList<ExtendedStatusInfo> supportedSwitches = new ArrayList<>();
    private ProgressDialog progressDialog;
    private Domoticz mDomoticz;
    private Context mActivity;
    private int currentSwitch = 1;
    private boolean infoDialogIsFavoriteSwitch;
    private boolean infoDialogIsFavoriteSwitchIsChanged = false;
    private SwitchesAdapter adapter;
    private ListView switchesListView;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        getActionBar().setTitle(R.string.title_switches);
    }

    @Override
    public void onConnectionOk() {
        showProgressDialog();

        mDomoticz = new Domoticz(mActivity);
        mDomoticz.getSwitches(new SwitchesReceiver() {
            @Override
            public void onReceiveSwitches(ArrayList<SwitchInfo> switches) {
                processSwitches(switches);
            }

            @Override
            public void onError(Exception error) {
                errorHandling(error);
            }
        });
    }

    private void processSwitches(ArrayList<SwitchInfo> switchInfos) {

        final ArrayList<ExtendedStatusInfo> extendedStatusSwitches = new ArrayList<>();
        final int totalNumberOfSwitches = switchInfos.size();

        for (SwitchInfo switchInfo : switchInfos) {
            successHandling(switchInfo.toString());
            int idx = switchInfo.getIdx();

            mDomoticz.getStatus(idx, new StatusReceiver() {
                @Override
                public void onReceiveStatus(ExtendedStatusInfo extendedStatusInfo) {
                    extendedStatusSwitches.add(extendedStatusInfo);     // Add to array
                    if (currentSwitch == totalNumberOfSwitches) {
                        createListView(extendedStatusSwitches);         // All extended info is in
                    }
                    else currentSwitch++;                               // Not there yet
                }

                @Override
                public void onError(Exception error) {
                    errorHandling(error);
                }
            });
        }
    }

    // add dynamic list view
    // https://github.com/nhaarman/ListViewAnimations
    private void createListView(ArrayList<ExtendedStatusInfo> switches) {

        final List<Integer> appSupportedSwitches = mDomoticz.getSupportedSwitches();

        for (ExtendedStatusInfo mExtendedStatusInfo : switches) {
            String name = mExtendedStatusInfo.getName();
            int switchTypeVal = mExtendedStatusInfo.getSwitchTypeVal();

            if (!name.startsWith(Domoticz.SWITCH_HIDDEN_CHARACTER) &&
                    appSupportedSwitches.contains(switchTypeVal)) {
                supportedSwitches.add(mExtendedStatusInfo);
            }
        }

        final switchesClickListener listener = this;

        adapter = new SwitchesAdapter(mActivity, supportedSwitches, listener);
        switchesListView = (ListView) getView().findViewById(R.id.listView);
        switchesListView.setAdapter(adapter);
        switchesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view,
                                           int index, long id) {
                showInfoDialog(supportedSwitches.get(index));
                return false;
            }
        });

        hideProgressDialog();
    }

    private void showInfoDialog(final ExtendedStatusInfo mSwitch) {

        switchInfoDialog infoDialog = new switchInfoDialog(
                getActivity(),
                mSwitch,
                R.layout.dialog_switch_info, this);
        infoDialog.setIdx(String.valueOf(mSwitch.getIdx()));
        infoDialog.setLastUpdate(mSwitch.getLastUpdate());
        infoDialog.setSignalLevel(String.valueOf(mSwitch.getSignalLevel()));
        infoDialog.setBatteryLevel(String.valueOf(mSwitch.getBatteryLevel()));
        infoDialog.setIsFavorite(mSwitch.getFavoriteBoolean());
        infoDialog.show();
        infoDialog.onDismissListener(new switchInfoDialog.InfoDialogDismissListener() {
            @Override
            public void onDismiss() {
                changeFavorite(mSwitch, infoDialogIsFavoriteSwitch);
            }
        });
    }

    private void changeFavorite(ExtendedStatusInfo mSwitch, boolean infoDialogIsFavoriteSwitch) {
        if (infoDialogIsFavoriteSwitchIsChanged) {
            mSwitch.setFavoriteBoolean(infoDialogIsFavoriteSwitch);
            int jsonAction;
            int jsonUrl = Domoticz.JSON_SET_URL_FAVORITE;

            if (infoDialogIsFavoriteSwitch) jsonAction = Domoticz.JSON_ACTION_FAVORITE_ON;
            else jsonAction = Domoticz.JSON_ACTION_FAVORITE_OFF;

            mDomoticz.setAction(mSwitch.getIdx(), jsonUrl, jsonAction, 0, new setCommandReceiver() {
                @Override
                public void onReceiveResult(String result) {
                    successHandling(result);
                }

                @Override
                public void onError(Exception error) {
                    // Domoticz always gives an error: ignore
                    errorHandling(error);
                }
            });
            infoDialogIsFavoriteSwitchIsChanged = false;
        }
    }

    @Override
    public void onSwitchClick(int idx, boolean checked) {
        Log.d(TAG, "handleSwitchClick");
        Log.d(TAG, "Set idx " + idx + " to " + checked);

        int jsonAction;
        int jsonUrl = Domoticz.JSON_SET_URL_SWITCHES;

        if (checked) jsonAction = Domoticz.JSON_ACTION_ON;
        else jsonAction = Domoticz.JSON_ACTION_OFF;

        mDomoticz.setAction(idx, jsonUrl, jsonAction, 0, new setCommandReceiver() {
            @Override
            public void onReceiveResult(String result) {
                successHandling(result);
            }

            @Override
            public void onError(Exception error) {
                errorHandling(error);
            }
        });
    }

    @Override
    public void onBlindClick(int idx, int action) {
        Log.d(TAG, "handleBlindsClick");

        int jsonUrl = Domoticz.JSON_SET_URL_SWITCHES;
        int jsonAction = Domoticz.JSON_ACTION_UP;

        switch (action) {
            case Domoticz.BLINDS_ACTION_UP:
                Log.d(TAG, "Set idx " + idx + " to up");
                jsonAction = Domoticz.JSON_ACTION_UP;
                break;

            case Domoticz.BLINDS_ACTION_STOP:
                Log.d(TAG, "Set idx " + idx + " to stop");
                jsonAction = Domoticz.JSON_ACTION_STOP;
                break;

            case Domoticz.BLINDS_ACTION_DOWN:
                Log.d(TAG, "Set idx " + idx + " to down");
                jsonAction = Domoticz.JSON_ACTION_DOWN;
                break;
        }

        mDomoticz.setAction(idx, jsonUrl, jsonAction, 0, new setCommandReceiver() {
            @Override
            public void onReceiveResult(String result) {
                Toast.makeText(getActivity(), R.string.action_success, Toast.LENGTH_LONG).show();
                successHandling(result);
            }

            @Override
            public void onError(Exception error) {
                errorHandling(error);
            }
        });
    }

    @Override
    public void onDimmerChange(int idx, int value) {

        Toast.makeText(mActivity, "Dimming to level: " + String.valueOf(value), Toast.LENGTH_SHORT).show();
    }

    /**
     * Notifies the list view adapter the data has changed and refreshes the list view
     */
    private void notifyDataSetChanged() {
        switchesListView.setAdapter(adapter);
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

    @Override
    public void onInfoDialogSwitchChange(int id, boolean isChecked) {
        infoDialogIsFavoriteSwitchIsChanged = true;
        infoDialogIsFavoriteSwitch = isChecked;
    }
}