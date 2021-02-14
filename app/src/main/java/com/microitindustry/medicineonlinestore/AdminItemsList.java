package com.microitindustry.medicineonlinestore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.microitindustry.medicineonlinestore.Common.Common;
import com.microitindustry.medicineonlinestore.Model.CategoryModel;
import com.microitindustry.medicineonlinestore.Model.ItemsListModel;
import com.microitindustry.medicineonlinestore.ViewHolder.CategoryAdminViewHolder;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminItemsList extends AppCompatActivity {

    FirebaseDatabase db;
    DatabaseReference dbRef;
    RecyclerView recyclerViewItem;
    LinearLayoutManager layoutManager;
    FirebaseRecyclerAdapter<ItemsListModel, CategoryAdminViewHolder> adapter;

    FirebaseStorage storage;
    StorageReference storageReference;

    EditText edtName,edtDesc,edtPrice;
    Button btnUpload,btnSelect;
    ItemsListModel newItem;
    Uri saveUri;

    RelativeLayout layoutRel;

    String categoryId="";

    // search functionality
    FirebaseRecyclerAdapter<ItemsListModel, CategoryAdminViewHolder> searchAdapter;
    List<String> suggestList =  new ArrayList<>();
    MaterialSearchBar materialSearchBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_items_list);

        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference("Items");
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        layoutRel = findViewById(R.id.layoutRel);
        recyclerViewItem = findViewById(R.id.recyclerAdminItems);
        layoutManager = new LinearLayoutManager(this);
        recyclerViewItem.setLayoutManager(layoutManager);



        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.addItems);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });

        if(getIntent() != null)
            categoryId = getIntent().getStringExtra("CategoryId");
        if(!categoryId.isEmpty())
            loadData(categoryId);


        //search
        materialSearchBar = findViewById(R.id.searchBar);
        loadSuggest();
        materialSearchBar.setLastSuggestions(suggestList);
        materialSearchBar.setCardViewElevation(10);
        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                List<String> suggest = new ArrayList<String>();
                for(String search:suggestList)
                {
                    if(search.toLowerCase().contains(materialSearchBar.getText().toLowerCase()))
                        suggest.add(search);
                }
                materialSearchBar.setLastSuggestions(suggest);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                List<String> suggest = new ArrayList<String>();
                for(String search:suggestList)
                {
                    if(search.toLowerCase().contains(materialSearchBar.getText().toLowerCase()))
                        suggest.add(search);
                }
                materialSearchBar.setLastSuggestions(suggest);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {

                if(!enabled){
                    recyclerViewItem.setAdapter(adapter);
                }
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                startSearch(text);
            }

            @Override
            public void onButtonClicked(int buttonCode) {

            }
        });
    }

    private void startSearch(CharSequence text) {
        searchAdapter = new FirebaseRecyclerAdapter<ItemsListModel, CategoryAdminViewHolder>(
                ItemsListModel.class,
                R.layout.main_items,
                CategoryAdminViewHolder.class,
                dbRef.orderByChild("itemName").equalTo(text.toString())
        ) {
            @Override
            protected void populateViewHolder(CategoryAdminViewHolder viewHolder, ItemsListModel model, int position) {
                viewHolder.ctName.setText(model.getItemName());
                viewHolder.ctDes.setText(model.getItemDescription());
                viewHolder.ctPrice.setText(model.getItemPrice());
                Picasso.with(getApplicationContext()).load(model.getImage())
                        .into(viewHolder.ctImage);


            }
        };
        recyclerViewItem.setAdapter(searchAdapter);
    }

    private void loadSuggest() {
        dbRef.orderByChild("itemName")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot postSnapshot:dataSnapshot.getChildren())
                        {
                            CategoryModel item = postSnapshot.getValue(CategoryModel.class);
                            suggestList.add(item.getItemName());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(item.getTitle().equals(Common.UPDATE))
        {
            showUpdateDialog(adapter.getRef(item.getOrder()).getKey(),adapter.getItem(item.getOrder()));
        }
        else if(item.getTitle().equals(Common.DELETE))
        {
            deleteOrder(adapter.getRef(item.getOrder()).getKey());

        }


        return super.onContextItemSelected(item);
    }

    private void showUpdateDialog(String key, ItemsListModel item) {
        AlertDialog.Builder alertDialog  =  new AlertDialog.Builder(AdminItemsList.this);
        alertDialog.setTitle("Update Category");
        alertDialog.setMessage("Please fill full information");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_menu_layout = inflater.inflate(R.layout.add_new_category_layout,null);



        edtName = add_menu_layout.findViewById(R.id.edtName);
        edtDesc = add_menu_layout.findViewById(R.id.edtDesc);
        edtPrice = add_menu_layout.findViewById(R.id.edtPrice);
        btnSelect = add_menu_layout.findViewById(R.id.btnSelect);
        btnUpload = add_menu_layout.findViewById(R.id.btnUpload);


        edtName.setText(item.getItemName());
        edtDesc.setText(item.getItemDescription());
        edtPrice.setText(item.getItemPrice());



        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();

            }
        });
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeImage();
            }
        });

        alertDialog.setView(add_menu_layout);
        alertDialog.setIcon(R.drawable.ic_baseline_add_box_24);

        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                if(newItem !=null)
                {
                    dbRef.child(key).setValue(newItem);
                    Snackbar.make(layoutRel,"Item " + newItem.getItemName()+" is updated",Snackbar.LENGTH_SHORT)
                            .show();
                }
            }
        });

        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        alertDialog.show();
    }

    private void changeImage() {
        if(saveUri !=null)
        {
            final ProgressDialog mDialog = new ProgressDialog(this);
            mDialog.setMessage("Uploading...");
            mDialog.show();

            String imageName = UUID.randomUUID().toString();
            final StorageReference imageFolder = storageReference.child("images/"+imageName);
            imageFolder.putFile(saveUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            mDialog.dismiss();
                            Toast.makeText(AdminItemsList.this, "Uploaded !!!", Toast.LENGTH_SHORT).show();
                            imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {

                                    newItem = new ItemsListModel(uri.toString(),edtName.getText().toString(),edtDesc.getText().toString(),edtPrice.getText().toString(),categoryId);

                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mDialog.dismiss();
                            Toast.makeText(AdminItemsList.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            mDialog.setMessage("Uploaded " + progress + "%");
                        }
                    });
        }
    }

    private void deleteOrder(String key) {
        dbRef.child(key).removeValue();
    }

    private void showDialog() {
        AlertDialog.Builder alertDialog  =  new AlertDialog.Builder(AdminItemsList.this);
        alertDialog.setTitle("Add new Item");
        alertDialog.setMessage("Please fill full information");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_menu_layout = inflater.inflate(R.layout.add_new_product_layout,null);


        edtName = add_menu_layout.findViewById(R.id.edtName);
        edtDesc = add_menu_layout.findViewById(R.id.edtDesc);
        edtPrice = add_menu_layout.findViewById(R.id.edtPrice);
        btnSelect = add_menu_layout.findViewById(R.id.btnSelect);
        btnUpload = add_menu_layout.findViewById(R.id.btnUpload);

        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();

            }
        });
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImage();
            }
        });

        alertDialog.setView(add_menu_layout);
        alertDialog.setIcon(R.drawable.ic_baseline_add_box_24);

        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                if(newItem !=null)
                {
                    dbRef.push().setValue(newItem);
                    Snackbar.make(layoutRel,"New Item " + newItem.getItemName()+" is added",Snackbar.LENGTH_SHORT)
                            .show();
                }
            }
        });

        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        alertDialog.show();
    }

    private void uploadImage() {
        if(saveUri !=null)
        {
            final ProgressDialog mDialog = new ProgressDialog(this);
            mDialog.setMessage("Uploading...");
            mDialog.show();

            String imageName = UUID.randomUUID().toString();
            final StorageReference imageFolder = storageReference.child("images/"+imageName);
            imageFolder.putFile(saveUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            mDialog.dismiss();
                            Toast.makeText(AdminItemsList.this, "Uploaded !!!", Toast.LENGTH_SHORT).show();
                            imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    newItem = new ItemsListModel(uri.toString(),edtName.getText().toString(),edtDesc.getText().toString(),edtPrice.getText().toString(),categoryId);

                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mDialog.dismiss();
                            Toast.makeText(AdminItemsList.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            mDialog.setMessage("Uploaded " + progress + "%");
                        }
                    });
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Common.PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null
        )
        {
            saveUri = data.getData();
            btnSelect.setText("Image Selected !");
        }
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Picture"), Common.PICK_IMAGE_REQUEST);
    }

    private void loadData(String categoryId) {
        adapter = new FirebaseRecyclerAdapter<ItemsListModel, CategoryAdminViewHolder>(
                ItemsListModel.class,
                R.layout.product_items,
                CategoryAdminViewHolder.class,
                dbRef.orderByChild("categoryId").equalTo(categoryId)
        ) {
            @Override
            protected void populateViewHolder(CategoryAdminViewHolder viewHolder, ItemsListModel model, int position) {

                viewHolder.ctName.setText(model.getItemName());
                viewHolder.ctDes.setText(model.getItemDescription());
                viewHolder.ctPrice.setText(model.getItemPrice());
                Picasso.with(getApplicationContext()).load(model.getImage())
                        .into(viewHolder.ctImage);

//                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Intent foodList =  new Intent(AdminItemsList.this, ProductActivity.class);
//                        foodList.putExtra("CategoryId",adapter.getRef(position).getKey());
//                        startActivity(foodList);
//                    }
//                });


            }
        };
        adapter.notifyDataSetChanged();
        recyclerViewItem.setAdapter(adapter);
    }
}