package com.example.bluepos.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.example.bluepos.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder> {

    private List<Reservation> reservations;
    private OnReservationActionListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnReservationActionListener {
        void onComplete(Reservation reservation);
        void onCancel(Reservation reservation);
        void onUndo(Reservation reservation);
    }

    public ReservationAdapter(List<Reservation> reservations, OnReservationActionListener listener) {
        this.reservations = reservations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reservation, parent, false);
        return new ReservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservationViewHolder holder, int position) {
        Reservation reservation = reservations.get(position);
        holder.tvCustomerName.setText(reservation.customerName);
        holder.tvProductName.setText(reservation.itemsSummary);
        holder.tvTotalPrice.setText(String.format(Locale.US, "Total: ₱%.2f", reservation.totalAmount));
        holder.tvPickupDate.setText("Date: " + dateFormat.format(new Date(reservation.timestamp)));
        holder.tvReservationStatus.setText(reservation.status.toUpperCase());

        // Update status background and button visibilities
        if ("Pending".equalsIgnoreCase(reservation.status)) {
            holder.tvReservationStatus.setBackgroundResource(R.drawable.bg_status_pending);
            holder.btnCompleteReservation.setVisibility(View.VISIBLE);
            holder.btnCancelReservation.setVisibility(View.VISIBLE);
            holder.btnUndoReservation.setVisibility(View.GONE);
        } else if ("Completed".equalsIgnoreCase(reservation.status)) {
            holder.tvReservationStatus.setBackgroundResource(R.drawable.bg_status_completed);
            holder.btnCompleteReservation.setVisibility(View.GONE);
            holder.btnCancelReservation.setVisibility(View.GONE);
            holder.btnUndoReservation.setVisibility(View.VISIBLE);
        } else { // Cancelled
            holder.tvReservationStatus.setBackgroundResource(R.drawable.bg_status_cancelled);
            holder.btnCompleteReservation.setVisibility(View.GONE);
            holder.btnCancelReservation.setVisibility(View.GONE);
            holder.btnUndoReservation.setVisibility(View.VISIBLE);
        }

        holder.btnCompleteReservation.setOnClickListener(v -> listener.onComplete(reservation));
        holder.btnCancelReservation.setOnClickListener(v -> listener.onCancel(reservation));
        holder.btnUndoReservation.setOnClickListener(v -> listener.onUndo(reservation));
    }

    @Override
    public int getItemCount() {
        return reservations.size();
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
        notifyDataSetChanged();
    }

    static class ReservationViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvProductName, tvTotalPrice, tvPickupDate, tvReservationStatus;
        MaterialButton btnCompleteReservation, btnCancelReservation, btnUndoReservation;

        public ReservationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvPickupDate = itemView.findViewById(R.id.tvPickupDate);
            tvReservationStatus = itemView.findViewById(R.id.tvReservationStatus);
            btnCompleteReservation = itemView.findViewById(R.id.btnCompleteReservation);
            btnCancelReservation = itemView.findViewById(R.id.btnCancelReservation);
            btnUndoReservation = itemView.findViewById(R.id.btnUndoReservation);
        }
    }
}
