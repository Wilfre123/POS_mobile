package com.example.bluepos.pos;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bluepos.R;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> productList;
    private OnAddToCartListener listener;
    private java.util.Map<Integer, Integer> cartQuantities = new java.util.HashMap<>();

    public interface OnAddToCartListener {
        void onAddToCart(Product product);
    }

    private boolean isLowStockMode = false;

    public ProductAdapter(List<Product> productList, OnAddToCartListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    public void setLowStockMode(boolean lowStockMode) {
        this.isLowStockMode = lowStockMode;
    }

    public void setCartQuantities(java.util.Map<Integer, Integer> quantities) {
        this.cartQuantities = quantities;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.tvName.setText(product.name);

        if (isLowStockMode) {
            holder.tvPrice.setVisibility(View.GONE);
            holder.tvAddedToCartCount.setVisibility(View.GONE);
            holder.btnAddToCart.setVisibility(View.GONE);
            holder.tvStock.setText("Qty: " + product.stock);
            holder.tvStock.setTextColor(Color.RED);
        } else {
            holder.tvPrice.setVisibility(View.VISIBLE);
            holder.tvPrice.setText(String.format(Locale.US, "₱%.2f", product.price));

            Integer addedCount = cartQuantities.get(product.id);
            if (addedCount == null) addedCount = 0;

            int remainingStock = product.stock - addedCount;
            holder.tvStock.setText("Stock: " + remainingStock);

            if (remainingStock <= product.minStock) {
                holder.tvStock.setTextColor(Color.RED);
            } else {
                holder.tvStock.setTextColor(Color.GRAY);
            }

            if (addedCount > 0) {
                holder.tvAddedToCartCount.setVisibility(View.VISIBLE);
                holder.tvAddedToCartCount.setText("Added: " + addedCount);
            } else {
                holder.tvAddedToCartCount.setVisibility(View.GONE);
            }
            holder.btnAddToCart.setVisibility(View.VISIBLE);
        }

        holder.btnAddToCart.setOnClickListener(v -> listener.onAddToCart(product));
        holder.itemView.findViewById(R.id.productItemLayout).setOnClickListener(v -> listener.onAddToCart(product));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void updateList(List<Product> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvStock, tvAddedToCartCount;
        Button btnAddToCart;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvStock = itemView.findViewById(R.id.tvProductStock);
            tvAddedToCartCount = itemView.findViewById(R.id.tvAddedToCartCount);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
        }
    }
}
