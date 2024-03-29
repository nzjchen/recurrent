package com.embit.recurrent.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.embit.recurrent.R;
import com.embit.recurrent.model.Item;
import com.google.android.material.card.MaterialCardView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Custom Item Adapter class to display item cards in the recycler view.
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private List<Item> itemList = new ArrayList<>();

    private String currency;


    public ItemAdapter(String currency) {
        this.currency = currency;
    }

    /**
     * Provides a reference to the type of views it is using.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final TextView name;
        private final TextView description;
        private final TextView amount;
        private final TextView nextOccurrence;
        private final TextView transactionType;
        private final TextView currencySymbol;

        public ViewHolder(View view) {
            super(view);

            card = (MaterialCardView) view.findViewById(R.id.card);
            name = (TextView) view.findViewById(R.id.name);
            description = (TextView) view.findViewById(R.id.description);
            amount = (TextView) view.findViewById(R.id.amount);
            nextOccurrence = (TextView) view.findViewById(R.id.nextOccurrence);
            transactionType = (TextView) view.findViewById(R.id.transactionType);
            currencySymbol = (TextView) view.findViewById(R.id.currencySymbol);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_item_card, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = itemList.get(position);
        holder.name.setText(item.getName());
        holder.description.setText(item.getDescription());
        holder.amount.setText(String.valueOf(item.getAmount()));
        holder.nextOccurrence.setText(item.getDaysUntilNextOccurrence() + " Days");
        holder.transactionType.setText(item.getType().name());
        holder.currencySymbol.setText(currency);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    /**
     * Set a new list of items as the displayed list in the recycler and notify observers.
     * @param itemList List of items to replace the old list.
     */
    public void setItemList(List<Item> itemList) {
        this.itemList = itemList;
        notifyDataSetChanged();
    }

    /**
     * Get the item object at a given position.
     * @param pos of item to get.
     * @return Item object at the given position.
     */
    public Item getItemAtPos(int pos) {
        return itemList.get(pos);
    }

}
