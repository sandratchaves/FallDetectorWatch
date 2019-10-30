package com.sandra.falldetector2.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sandra.falldetector2.App;
import com.sandra.falldetector2.R;
import com.sandra.falldetector2.model.Contact;

public class ContactAdpter extends RecyclerView.Adapter<ContactAdpter.ContactViewHolder> {

    private Contact[] contacts;

    public ContactAdpter(Contact[] contacts){
        this.contacts = contacts;
        Log.d("adpter", "ContactAdpter: " + contacts.length);
    }

    @NonNull
    @Override
    public ContactAdpter.ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View itemContact = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_list,parent,false);
        return new ContactViewHolder(itemContact);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactAdpter.ContactViewHolder contactViewHolder, int i) {
        Contact c = contacts[i];
        if (c!= null){
            contactViewHolder.name.setText(c.getName());
            contactViewHolder.email.setText(c.getNumber());
            contactViewHolder.isImportant.setChecked(c.isImportant());
        }
    }

    @Override
    public int getItemCount() {
        Log.d("adpter", "getItemCount: " + contacts.length);
        return contacts.length;
    }

    public class ContactViewHolder extends RecyclerView.ViewHolder{

        TextView name;
        TextView email;
        ImageButton removeBtn;
        Switch isImportant;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            email = itemView.findViewById(R.id.mail);
            removeBtn = itemView.findViewById(R.id.removeButton);
            isImportant = itemView.findViewById(R.id.switch_important);
            removeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    App.getInstance().getContactRepository().removeContact(contacts[getAdapterPosition()]);
                    contacts = App.getInstance().getContactRepository().getAllContacts();
                    notifyDataSetChanged();
                }
            });
            isImportant.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked){
                        App.getInstance().getContactRepository().updateContact(contacts[getAdapterPosition()]);
                        contacts = App.getInstance().getContactRepository().getAllContacts();
                        notifyDataSetChanged();
                    }
                }
            });

        }
    }
}