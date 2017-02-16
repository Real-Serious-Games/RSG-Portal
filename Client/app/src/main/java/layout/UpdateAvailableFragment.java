package layout;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.rsg.rsgportal.R;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link UpdateAvailableFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link UpdateAvailableFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UpdateAvailableFragment extends Fragment {
    private static final String ARG_CURRENT_VERSION = "currentVersion";
    private static final String ARG_NEW_VERSION = "newVersion";
    private static final String ARG_DOWNLOAD_SIZE = "downloadSize";
    private static final String ARG_AVAILABLE_VERSIONS = "availableVersions";

    private String currentVersion;
    private String newVersion;
    private String downloadSize;
    private ArrayList<String> availableVersions;

    private OnFragmentInteractionListener mListener;

    public UpdateAvailableFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment UpdateAvailableFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static UpdateAvailableFragment newInstance(String currentVersion,
                                                      String newVersion,
                                                      ArrayList<String> availableVersions,
                                                      String downloadSize) {
        UpdateAvailableFragment fragment = new UpdateAvailableFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CURRENT_VERSION, currentVersion);
        args.putString(ARG_NEW_VERSION, newVersion);
        args.putStringArrayList(ARG_AVAILABLE_VERSIONS, availableVersions);
        args.putString(ARG_DOWNLOAD_SIZE, downloadSize);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentVersion = getArguments().getString(ARG_CURRENT_VERSION);
            newVersion = getArguments().getString(ARG_NEW_VERSION);
            availableVersions = getArguments().getStringArrayList(ARG_AVAILABLE_VERSIONS);
            downloadSize = getArguments().getString(ARG_DOWNLOAD_SIZE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_update_available, container, false);

        Button downloadButton = (Button) view.findViewById(R.id.download_button);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onDownloadClicked();
                }
            }
        });

        TextView currentVersionText = (TextView) view.findViewById(R.id.current_version);
        currentVersionText.setText(currentVersion);

        Spinner versionSpinner = (Spinner) view.findViewById(R.id.version_spinner);
        ArrayAdapter<String> spinnerArrayAdapter =
                new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, availableVersions);
        versionSpinner.setAdapter(spinnerArrayAdapter);

        // Find index of new version in list of available versions
        int index = -1;
        for (int i = 0; i < availableVersions.size(); i++) {
            if (newVersion == availableVersions.get(i)) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            versionSpinner.setSelection(index);
        } else {
            versionSpinner.setSelection(availableVersions.size() - 1);
        }

        // Let parent activity know when an item is clicked on.
        versionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mListener.onVersionSelectionChanged(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        TextView downloadSizeText = (TextView) view.findViewById(R.id.download_size);
        downloadSizeText.setText(downloadSizeText.getText() + downloadSize);

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onDownloadClicked();

        void onVersionSelectionChanged(int newVersion);
    }
}
