package com.hippo.ehviewer.ui.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.hippo.easyrecyclerview.LinearDividerItemDecoration;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.Hosts;
import com.hippo.ehviewer.R;
import com.hippo.view.ViewTransition;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.ViewUtils;

import java.util.List;
import java.util.Locale;

import rikka.core.res.ResourcesKt;

public class HostsFragment extends BaseFragment
        implements View.OnClickListener {

    private static final String DIALOG_TAG_ADD_HOST = AddHostDialogFragment.class.getName();
    private static final String DIALOG_TAG_EDIT_HOST = EditHostDialogFragment.class.getName();

    private static final String KEY_HOST = "com.hippo.ehviewer.ui.fragment.HostsFragment.HOST";
    private static final String KEY_IP = "com.hippo.ehviewer.ui.fragment.HostsFragment.IP";

    private Hosts hosts;
    private List<Pair<String, String>> data;

    private ViewTransition mViewTransition;
    private HostsAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hosts = EhApplication.getHosts(requireContext());
        data = hosts.getAll();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_hosts, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        TextView tip = (TextView) ViewUtils.$$(view, R.id.tip);
        mViewTransition = new ViewTransition(recyclerView, tip);
        FloatingActionButton fab = view.findViewById(R.id.fab);

        adapter = new HostsAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false));
        LinearDividerItemDecoration decoration = new LinearDividerItemDecoration(
                LinearDividerItemDecoration.VERTICAL,
                ResourcesKt.resolveColor(requireActivity().getTheme(), R.attr.dividerColor),
                LayoutUtils.dp2pix(requireActivity(), 1));
        decoration.setShowLastDivider(true);
        recyclerView.addItemDecoration(decoration);
        recyclerView.setHasFixedSize(true);

        fab.setOnClickListener(this);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateView(false);
    }

    public boolean onItemClick(int position) {
        Pair<String, String> pair = data.get(position);
        Bundle args = new Bundle();
        args.putString(KEY_HOST, pair.first);
        args.putString(KEY_IP, pair.second);

        DialogFragment fragment = new EditHostDialogFragment();
        fragment.setArguments(args);
        fragment.show(getChildFragmentManager(), DIALOG_TAG_EDIT_HOST);

        return true;
    }

    @Override
    public void onClick(View v) {
        new AddHostDialogFragment().show(getChildFragmentManager(), DIALOG_TAG_ADD_HOST);
    }

    private void updateView(boolean animation) {
        if (null == mViewTransition) {
            return;
        }

        data = hosts.getAll();
        if (data.isEmpty()) {
            mViewTransition.showView(1, animation);
        } else {
            mViewTransition.showView(0, animation);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public int getFragmentTitle() {
        return R.string.hosts;
    }

    public abstract static class HostDialogFragment extends DialogFragment {

        private HostsFragment hostsFragment;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            hostsFragment = (HostsFragment) getParentFragment();
            View view = getLayoutInflater().inflate(R.layout.dialog_hosts, null, false);
            TextView host = view.findViewById(R.id.host);
            TextView ip = view.findViewById(R.id.ip);

            Bundle arguments = getArguments();
            if (savedInstanceState == null && arguments != null) {
                host.setText(arguments.getString(KEY_HOST));
                ip.setText(arguments.getString(KEY_IP));
            }

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext()).setView(view);
            onCreateDialogBuilder(builder);
            AlertDialog dialog = builder.create();
            dialog.setOnShowListener(d -> onCreateDialog((AlertDialog) d));

            return dialog;
        }

        protected abstract void onCreateDialogBuilder(AlertDialog.Builder builder);

        protected abstract void onCreateDialog(AlertDialog dialog);

        protected void put(AlertDialog dialog) {
            TextView host = dialog.findViewById(R.id.host);
            TextView ip = dialog.findViewById(R.id.ip);
            if (host == null || ip == null) {
                return;
            }
            String hostString = host.getText().toString().trim().toLowerCase(Locale.US);
            String ipString = ip.getText().toString().trim();

            if (!Hosts.isValidHost(hostString)) {
                TextInputLayout hostInputLayout = dialog.findViewById(R.id.host_input_layout);
                if (hostInputLayout == null) {
                    return;
                }
                hostInputLayout.setError(getString(R.string.invalid_host));
                return;
            }

            if (!Hosts.isValidIp(ipString)) {
                TextInputLayout ipInputLayout = dialog.findViewById(R.id.ip_input_layout);
                if (ipInputLayout == null) {
                    return;
                }
                ipInputLayout.setError(getString(R.string.invalid_ip));
                return;
            }

            hostsFragment.hosts.put(hostString, ipString);
            hostsFragment.updateView(true);

            dialog.dismiss();
        }

        protected void delete(AlertDialog dialog) {
            TextView host = dialog.findViewById(R.id.host);
            if (host == null) {
                return;
            }
            String hostString = host.getText().toString().trim().toLowerCase(Locale.US);

            hostsFragment.hosts.delete(hostString);
            hostsFragment.updateView(true);

            dialog.dismiss();
        }
    }

    public static class AddHostDialogFragment extends HostDialogFragment {

        @Override
        protected void onCreateDialogBuilder(AlertDialog.Builder builder) {
            builder.setTitle(R.string.add_host);
            builder.setPositiveButton(R.string.add_host_add, null);
            builder.setNegativeButton(android.R.string.cancel, null);
        }

        @Override
        protected void onCreateDialog(AlertDialog dialog) {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> put(dialog));
        }
    }

    public static class EditHostDialogFragment extends HostDialogFragment {

        @Override
        protected void onCreateDialogBuilder(AlertDialog.Builder builder) {
            builder.setTitle(R.string.edit_host);
            builder.setPositiveButton(R.string.edit_host_confirm, null);
            builder.setNegativeButton(R.string.edit_host_delete, null);
        }

        @Override
        protected void onCreateDialog(AlertDialog dialog) {
            TextInputLayout hostInputLayout = dialog.findViewById(R.id.host_input_layout);
            if (hostInputLayout == null) {
                return;
            }
            hostInputLayout.setEnabled(false);
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> put(dialog));
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> delete(dialog));
        }
    }

    private static class HostsHolder extends RecyclerView.ViewHolder {

        public final TextView host;
        public final TextView ip;

        public HostsHolder(View itemView) {
            super(itemView);
            host = itemView.findViewById(R.id.host);
            ip = itemView.findViewById(R.id.ip);
        }
    }

    private class HostsAdapter extends RecyclerView.Adapter<HostsHolder> {

        private final LayoutInflater inflater = getLayoutInflater();

        @NonNull
        @Override
        public HostsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new HostsHolder(inflater.inflate(R.layout.item_hosts, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull HostsHolder holder, int position) {
            Pair<String, String> pair = data.get(position);
            holder.host.setText(pair.first);
            holder.ip.setText(pair.second);
            holder.itemView.setOnClickListener(v -> onItemClick(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
