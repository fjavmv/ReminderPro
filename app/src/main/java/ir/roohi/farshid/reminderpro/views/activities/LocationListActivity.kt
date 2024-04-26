package ir.roohi.farshid.reminderpro.views.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import ir.roohi.farshid.reminderpro.R
import ir.roohi.farshid.reminderpro.customViews.AlertDialog
import ir.roohi.farshid.reminderpro.databinding.ActivityReminderLocationBinding
import ir.roohi.farshid.reminderpro.databinding.ItemLocationBinding
import ir.roohi.farshid.reminderpro.databinding.LayoutItemSelectedBinding
import ir.roohi.farshid.reminderpro.listener.OnClickItemLocationListener
import ir.roohi.farshid.reminderpro.listener.OnInformationLocationListener
import ir.roohi.farshid.reminderpro.listener.multiSelect.OnMultiSelectLocationListener
import ir.roohi.farshid.reminderpro.model.LocationEntity
import ir.roohi.farshid.reminderpro.service.UserLocationService
import ir.roohi.farshid.reminderpro.utility.LocationUtility
import ir.roohi.farshid.reminderpro.viewModel.LocationViewModel
import ir.roohi.farshid.reminderpro.views.adapter.LocationAdapter
import ir.roohi.farshid.reminderpro.views.bottomSheet.InformationLocationBottomSheet
//import kotlinx.android.synthetic.main.activity_reminder_location.*
//import kotlinx.android.synthetic.main.layout_item_selected.*
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList


/**
 * Created by Farshid Roohi.
 * ReminderPro | Copyrights 2018.
 */
class LocationListActivity : BaseActivity(), OnMultiSelectLocationListener {

    private lateinit var adapter: LocationAdapter
    private lateinit var viewModel: LocationViewModel
    private lateinit var binding: ActivityReminderLocationBinding
    private lateinit var bindingItem: LayoutItemSelectedBinding
    //  private lateinit var bindingLocation: ActivityReminderLocationBinding

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, LocationListActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_location)

        viewModel = ViewModelProviders.of(this).get(LocationViewModel::class.java)

        adapter = LocationAdapter()
        adapter.listenerMultiSelect = this
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        adapter.onClickListener = object : OnClickItemLocationListener {
            override fun onClickItemLocation(position: Int, element: LocationEntity) {
                val flag = LocationUtility(this@LocationListActivity)
                    .isFarLocation(element.latitude, element.longitude, element.distance)
                if (!flag) {
                    Snackbar.make(
                        binding.recycler,
                        String.format(
                            getString(R.string.error_distance_the_selected_location_near),
                            element.distance
                        ),
                        Snackbar.LENGTH_LONG
                    ).show()
                    element.status = false
                    viewModel.update(element)
                    resetData()
                    return
                }

                viewModel.update(element)
                val list: ArrayList<LocationEntity> = ArrayList(adapter.items!!)
                list[position] = element
                val intent = Intent(this@LocationListActivity, UserLocationService::class.java)
                intent.putExtra("locationEntity", list as Serializable)
                startService(intent)
            }
        }
        viewModel.liveDateLocations!!.observe(this, Observer<List<LocationEntity>> { list ->
            binding.layoutEmptyState.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            if (list == null || list.isEmpty()) {
                adapter.removeAll()
                binding.layoutEmptyState.visibility = View.VISIBLE
                return@Observer
            }
            adapter.removeAll()
            adapter.addItems(ArrayList(list))
//            if (list.size != adapter.itemCount) {
//                adapter.removeAll()
//                adapter.addItems(ArrayList(list))
//            }

        })

        binding.toolbar.getLeftImageView().setOnClickListener {
            finish()
        }
        binding.fabAdd.setOnClickListener {
            SelectPlaceActivity.start(this)
        }
    }


    override fun onMultiSelectLocation(items: ArrayList<LocationEntity>) {
        if (items.size == 0) {
            bindingItem.layoutSelectItem.visibility = View.GONE
            return
        }
        bindingItem.txtCounterSelect.text =
            String.format(getString(R.string.selected_number), items.size.toString())
        setStatusBarColor(R.color.black)
        if (bindingItem.layoutSelectItem.visibility == View.GONE) {
            bindingItem.layoutSelectItem.visibility = View.VISIBLE
        }

        if (items.size > 1) {
            bindingItem.imgShare.visibility = View.GONE
            bindingItem.imgEdit.visibility = View.GONE
        } else {
            bindingItem.imgShare.visibility = View.VISIBLE
            bindingItem.imgEdit.visibility = View.VISIBLE
        }

        bindingItem.imgEdit.setOnClickListener {
            edit(items.first())
        }

        bindingItem.imgCancelSelect.setOnClickListener {
            resetData()
        }
        bindingItem.imgShare.setOnClickListener {
            if (items.isEmpty()) {
                return@setOnClickListener
            }
            val uri =
                String.format(Locale.ENGLISH, "geo:%f,%f", items[0].latitude, items[0].longitude)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            startActivity(intent)

        }
        bindingItem.imgDelete.setOnClickListener {
            val alertDialog = AlertDialog.Builder(
                supportFragmentManager,
                getString(R.string.note), getString(R.string.do_you_sure_delete)
            )
            alertDialog.setBtnNegative(getString(R.string.no), View.OnClickListener {
                alertDialog.dialog!!.dismiss()
            })
            alertDialog.setBtnPositive(getString(R.string.yes), View.OnClickListener {
                items.forEach { item ->
                    viewModel.remove(item)
                }
                bindingItem.layoutSelectItem.visibility = View.GONE
                setStatusBarColor(R.color.colorPrimaryDark)
                items.clear()
                alertDialog.dialog!!.dismissAllowingStateLoss()
            })
            alertDialog.build().show()

        }
    }

    private fun resetData() {
        adapter.itemsSelected.clear()
        adapter.items?.forEach { item ->
            item.statusSelect = false
        }
        adapter.notifyDataSetChanged()
        bindingItem.layoutSelectItem.visibility = View.GONE
        setStatusBarColor(R.color.colorPrimaryDark)
    }

    private fun edit(item: LocationEntity) {
        val bottomSheet =
            InformationLocationBottomSheet(
                supportFragmentManager,
                object : OnInformationLocationListener {
                    override fun onInformationLocation(
                        title: String,
                        desc: String?,
                        distance: Int
                    ) {
                        item.distance = distance
                        item.title = title
                        item.text = desc
                        viewModel.update(item)
                        resetData()
                    }
                })
        bottomSheet.modelMap = item
        bottomSheet.show()
    }

    override fun onBackPressed() {
        if (bindingItem.layoutSelectItem.visibility == View.VISIBLE) {
            resetData()
            return
        }
        super.onBackPressed()
    }

}
