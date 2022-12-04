/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package com.hippo.ehviewer.ui.fragment

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.hippo.app.BaseDialogBuilder
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.Hosts
import com.hippo.ehviewer.R
import com.hippo.view.ViewTransition

class HostsFragment : BaseFragment(), View.OnClickListener {
    private var hosts: Hosts? = null
    private var data: List<Pair<String, String>>? = null
    private var mViewTransition: ViewTransition? = null
    private var adapter: HostsAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EhApplication.hosts.apply {
            hosts = this
            data = all
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val recyclerView = inflater.inflate(R.layout.rv_layout, container, false) as RecyclerView
        val tip = getTipView(R.string.hosts)
        mViewTransition = ViewTransition(recyclerView, tip)
        val fab = getFabViewAndShow()
        adapter = HostsAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        recyclerView.setHasFixedSize(true)
        fab.setOnClickListener(this)
        return recyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateView(false)
    }

    fun onItemClick(position: Int): Boolean {
        val pair = data!![position]
        val args = Bundle()
        args.putString(KEY_HOST, pair.first)
        args.putString(KEY_IP, pair.second)
        val fragment: DialogFragment = EditHostDialogFragment()
        fragment.arguments = args
        fragment.show(childFragmentManager, DIALOG_TAG_EDIT_HOST)
        return true
    }

    override fun onClick(v: View) {
        AddHostDialogFragment().show(childFragmentManager, DIALOG_TAG_ADD_HOST)
    }

    private fun updateView(animation: Boolean) {
        if (null == mViewTransition) {
            return
        }
        data = hosts!!.all
        if ((data as MutableList<Pair<String, String>>?)?.isEmpty() == true) {
            mViewTransition!!.showView(1, animation)
        } else {
            mViewTransition!!.showView(0, animation)
        }
        adapter!!.notifyDataSetChanged()
    }

    override fun getFragmentTitle(): Int {
        return R.string.hosts
    }

    abstract class HostDialogFragment : DialogFragment() {
        private var hostsFragment: HostsFragment? = null
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            hostsFragment = parentFragment as HostsFragment?
            val view = layoutInflater.inflate(R.layout.dialog_hosts, null, false)
            val host = view.findViewById<TextView>(R.id.host)
            val ip = view.findViewById<TextView>(R.id.ip)
            val arguments = arguments
            if (savedInstanceState == null && arguments != null) {
                host.text = arguments.getString(KEY_HOST)
                ip.text = arguments.getString(KEY_IP)
            }
            val builder = BaseDialogBuilder(requireContext())
            builder.setView(view)
            onCreateDialogBuilder(builder)
            val dialog = builder.create()
            dialog.setOnShowListener { d: DialogInterface -> onCreateDialog(d as AlertDialog) }
            return dialog
        }

        protected abstract fun onCreateDialogBuilder(builder: AlertDialog.Builder)
        protected abstract fun onCreateDialog(dialog: AlertDialog)
        protected fun put(dialog: AlertDialog) {
            val host = dialog.findViewById<TextView>(R.id.host)
            val ip = dialog.findViewById<TextView>(R.id.ip)
            if (host == null || ip == null) {
                return
            }
            val hostString = host.text.toString().trim { it <= ' ' }.lowercase()
            val ipString = ip.text.toString().trim { it <= ' ' }
            if (!Hosts.isValidHost(hostString)) {
                val hostInputLayout =
                    dialog.findViewById<TextInputLayout>(R.id.host_input_layout) ?: return
                hostInputLayout.error = getString(R.string.invalid_host)
                return
            }
            if (!Hosts.isValidIp(ipString)) {
                val ipInputLayout =
                    dialog.findViewById<TextInputLayout>(R.id.ip_input_layout) ?: return
                ipInputLayout.error = getString(R.string.invalid_ip)
                return
            }
            hostsFragment!!.hosts!!.put(hostString, ipString)
            hostsFragment!!.updateView(true)
            dialog.dismiss()
        }

        protected fun delete(dialog: AlertDialog) {
            val host = dialog.findViewById<TextView>(R.id.host) ?: return
            val hostString = host.text.toString().trim { it <= ' ' }.lowercase()
            hostsFragment!!.hosts!!.delete(hostString)
            hostsFragment!!.updateView(true)
            dialog.dismiss()
        }
    }

    class AddHostDialogFragment : HostDialogFragment() {
        override fun onCreateDialogBuilder(builder: AlertDialog.Builder) {
            builder.setTitle(R.string.add_host)
            builder.setPositiveButton(R.string.add_host_add, null)
            builder.setNegativeButton(android.R.string.cancel, null)
        }

        override fun onCreateDialog(dialog: AlertDialog) {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener { put(dialog) }
        }
    }

    class EditHostDialogFragment : HostDialogFragment() {
        override fun onCreateDialogBuilder(builder: AlertDialog.Builder) {
            builder.setTitle(R.string.edit_host)
            builder.setPositiveButton(R.string.edit_host_confirm, null)
            builder.setNegativeButton(R.string.edit_host_delete, null)
        }

        override fun onCreateDialog(dialog: AlertDialog) {
            val hostInputLayout =
                dialog.findViewById<TextInputLayout>(R.id.host_input_layout) ?: return
            hostInputLayout.isEnabled = false
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener { put(dialog) }
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                .setOnClickListener { delete(dialog) }
        }
    }

    private class HostsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val host: TextView
        val ip: TextView

        init {
            host = itemView.findViewById(R.id.host)
            ip = itemView.findViewById(R.id.ip)
        }
    }

    private inner class HostsAdapter : RecyclerView.Adapter<HostsHolder>() {
        private val inflater = layoutInflater
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HostsHolder {
            return HostsHolder(inflater.inflate(R.layout.item_hosts, parent, false))
        }

        override fun onBindViewHolder(holder: HostsHolder, position: Int) {
            val pair = data!![position]
            holder.host.text = pair.first
            holder.ip.text = pair.second
            holder.itemView.setOnClickListener { onItemClick(position) }
        }

        override fun getItemCount(): Int {
            return data!!.size
        }
    }

    companion object {
        private val DIALOG_TAG_ADD_HOST = AddHostDialogFragment::class.java.name
        private val DIALOG_TAG_EDIT_HOST = EditHostDialogFragment::class.java.name
        private const val KEY_HOST = "com.hippo.ehviewer.ui.fragment.HostsFragment.HOST"
        private const val KEY_IP = "com.hippo.ehviewer.ui.fragment.HostsFragment.IP"
    }
}