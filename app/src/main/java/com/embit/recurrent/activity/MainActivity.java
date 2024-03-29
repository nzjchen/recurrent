package com.embit.recurrent.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.embit.recurrent.R;
import com.embit.recurrent.adapter.ItemAdapter;
import com.embit.recurrent.model.Item;
import com.embit.recurrent.model.ItemViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Currency;
import java.util.Locale;


/**
 * Main activity of the app.
 */
public class MainActivity extends AppCompatActivity {
    public static final int ADD_ITEM_REQUEST = 1;
    public static final int EDIT_ITEM_REQUEST = 2;
    public static final String EXTRA_EDIT_ITEM = "EDIT_ITEM";

    private final String CHANNEL_ID = "channel_1";

    private ItemViewModel itemViewModel;

    private RecyclerView recycler;
    private ItemAdapter itemAdapter;

    private Disposable getItemsDisposable;
    private Disposable itemUpdateOccurrenceDisposable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set app wide currency symbol to phone's local.
        Currency localeCurrency = Currency.getInstance(Locale.getDefault());
        this.getSharedPreferences(getString(R.string.SHARED_PREF_KEY), Context.MODE_PRIVATE)
                .edit()
                .putString("localeSymbol", localeCurrency.getSymbol())
                .apply();

        // Check Theme
        switch (PreferenceManager.getDefaultSharedPreferences(this).getString("theme_pref", "")) {
            case "Light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "Dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }



        // Add new item.
        FloatingActionButton buttonAddItem = findViewById(R.id.fabAddItem);
        buttonAddItem.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), AddEditItemActivity.class);
            startActivityForResult(intent, ADD_ITEM_REQUEST);
        });

        /// Recycler View
        recycler = findViewById(R.id.itemRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setHasFixedSize(true);
        itemAdapter = new ItemAdapter(this.getSharedPreferences(getString(R.string.SHARED_PREF_KEY), Context.MODE_PRIVATE).getString("localeSymbol", "$"));
        recycler.setAdapter(itemAdapter);

        itemViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication())).get(ItemViewModel.class);

        itemUpdateOccurrenceDisposable = itemViewModel.getAllItems().toObservable().firstElement().subscribe(itemList -> {
            Log.d("Item Update Disposable", "Updating Item Occurrences.");
            for (Item item: itemList) {
                item.updateOccurrence();
                Completable.fromRunnable(() -> itemViewModel.update(item))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe();
            }
        });

        getItemsDisposable = itemViewModel.getAllItems()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(itemList -> itemAdapter.setItemList(itemList), Throwable::printStackTrace);


        // Card movement that does actions depending on swiping direction.
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Delete
                if (direction == ItemTouchHelper.RIGHT) {
                    Item deletedItem = itemAdapter.getItemAtPos(viewHolder.getAdapterPosition());

                    Completable.fromRunnable(() -> itemViewModel.delete(deletedItem))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe();
                    Snackbar.make(recycler, "Item was deleted.", Snackbar.LENGTH_SHORT)
                            .setAction("Undo", view -> Completable.fromRunnable(() ->
                                    itemViewModel.insert(deletedItem))
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe())
                            .show();
                }
                // Edit
                else if (direction == ItemTouchHelper.LEFT) {
                    Item item = itemAdapter.getItemAtPos(viewHolder.getAdapterPosition());
                    itemAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                    Intent intent = new Intent(MainActivity.this, AddEditItemActivity.class);
                    intent.putExtra(MainActivity.EXTRA_EDIT_ITEM, item);
                    startActivityForResult(intent, EDIT_ITEM_REQUEST);
                }
            }

            @SuppressLint("ResourceAsColor")
            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addSwipeRightBackgroundColor(getColor(R.color.colorDelete))
                        .addSwipeRightActionIcon(R.drawable.ic_delete)
                        .addSwipeLeftBackgroundColor(getColor(R.color.colorEdit))
                        .addSwipeLeftActionIcon(R.drawable.ic_edit)
                        .create()
                        .decorate();

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }).attachToRecyclerView(recycler);

        // Sorting Chip
        Chip sortChip = findViewById(R.id.sortChip);
        sortChip.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(getApplicationContext(), view);
            MenuInflater inflater = popupMenu.getMenuInflater();
            inflater.inflate(R.menu.menu_sort_items, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                switch (menuItem.getItemId()) {
                    // Sort Menu
                    case R.id.sortAlphabetical:
                        itemViewModel.getAllItemsByName()
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(itemList -> itemAdapter.setItemList(itemList));
                        return true;

                    case R.id.sortAmount:
                        itemViewModel.getAllItemsByAmount()
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(itemList -> itemAdapter.setItemList(itemList));
                        return true;

                    case R.id.sortNextOccurrence:
                        itemViewModel.getAllItemsByNextOccurrence()
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(itemList -> itemAdapter.setItemList(itemList));
                        return true;

                    default:
                        return false;

                }
            });
            popupMenu.show();
        });

        Chip settingsChip = findViewById(R.id.settingsChip);
        settingsChip.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });


//        Notifications Testing.
//        Intent intent = new Intent(this, BroadcastManager.class);
//        PendingIntent pending = PendingIntent.getBroadcast(this, 42, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        AlarmManager manager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
//
//        Calendar c = Calendar.getInstance();
//        LocalDateTime now = LocalDateTime.now();
//        c.set(Calendar.HOUR_OF_DAY, now.getHour());
//        c.set(Calendar.MINUTE, now.getMinute() + 2);
//        c.set(Calendar.SECOND, 0);
//
//        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pending);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_ITEM_REQUEST && resultCode == RESULT_OK) {
            Item newItem = data.getParcelableExtra(AddEditItemActivity.EXTRA_SAVED_ITEM);
            Completable.fromRunnable(() -> itemViewModel.insert(newItem))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();

        }
        else if (requestCode == EDIT_ITEM_REQUEST && resultCode == RESULT_OK) {
            Item editedItem = data.getParcelableExtra(AddEditItemActivity.EXTRA_SAVED_ITEM);
            Completable.fromRunnable(() -> itemViewModel.update(editedItem))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
            Toast.makeText(this, "Successfully edited item.", Toast.LENGTH_SHORT).show();
        }

    }



//    /** Reimplementing notifications at a later time.
//     * Create a new notification channel, but only on API 26+.
//     * @return notification manager, if device API is 26+.
//     */
//    private NotificationManager createNotificationChannel() {
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            String description = "Main Channel";
//            int importance = NotificationManager.IMPORTANCE_DEFAULT;
//
//            NotificationChannel channel = new NotificationChannel("channel1", CHANNEL_ID, importance);
//            channel.setDescription(description);
//
//            NotificationManager notificationManager = getSystemService(NotificationManager.class);
//            notificationManager.createNotificationChannel(channel);
//            return notificationManager;
//        }
//        return null;
//    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        itemUpdateOccurrenceDisposable.dispose();
        getItemsDisposable.dispose();
    }
}