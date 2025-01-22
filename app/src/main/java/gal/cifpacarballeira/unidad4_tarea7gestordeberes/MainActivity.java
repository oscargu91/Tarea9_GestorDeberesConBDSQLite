package gal.cifpacarballeira.unidad4_tarea7gestordeberes;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import java.util.List;
import android.database.Cursor;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HomeworkAdapter adapter;
    private List<Homework> homeworkList;

    private SQLiteDatabase bdLectura;
    private SQLiteDatabase bdEscribir;
    private ContentValues contentValues;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Inicialización de componentes
        recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton fab = findViewById(R.id.fab);
        homeworkList = new ArrayList<>();

        BaseDeDatos bd = new BaseDeDatos(this);
        bdLectura = bd.getReadableDatabase();
        bdEscribir = bd.getWritableDatabase();
        contentValues = new ContentValues();
        generarDatos();

        // Crear y configurar el adaptador
        adapter = new HomeworkAdapter(homeworkList, homework -> showBottomSheet(homework));


        // Este código sería lo mismo que la anterior línea
        // adapter = new HomeworkAdapter(homeworkList, this::showBottomSheet);
        // ¿Por qué le paso ese segundo parámetro?
        // Porque le estoy pasando la función que quiero que se lance al hacer click en un elemento
        // Investiga sobre "operador de referencia de método en Java"


        // Configuración del RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Configuración del botón flotante
        fab.setOnClickListener(v -> showAddHomeworkDialog(null));
    }

    private void generarDatos() {
        String asignatura = "";
        String descripcion = "";
        String fecha = "";
        Boolean estado = false;
        Integer id = 0;
        String consulta = "SELECT * from tablaDeberes";
        Cursor cursor = bdLectura.rawQuery(consulta, null);

        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(0);
                asignatura = cursor.getString(1);
                descripcion = cursor.getString(2);
                fecha = cursor.getString(3);
                estado = (cursor.getInt(4) == 1);
                Homework homework = new Homework(asignatura, descripcion, fecha, estado, id);
                homeworkList.add(homework);
            } while (cursor.moveToNext());  // Mueve el cursor al siguiente registro
            cursor.close();
        }
    }


    private void showAddHomeworkDialog(Homework homeworkToEdit) {

        NewHomeworkDialogFragment dialog = new NewHomeworkDialogFragment();
        // Pasarle el objeto Homework al diálogo si se está editando
        if (homeworkToEdit != null) {
            Bundle args = new Bundle();
            args.putParcelable("homework", homeworkToEdit);
            dialog.setArguments(args);
        }
        dialog.setOnHomeworkSavedListener(homework -> {
            if (homeworkToEdit == null) {
                // Si es una tarea nueva, se agrega a la lista y se guarda en la base de datos
                homeworkList.add(homework);
                contentValues.put("asignatura", homework.getSubject());
                contentValues.put("descripcion", homework.getDescription());
                contentValues.put("estadoTarea", homework.isCompleted());
                contentValues.put("fechaEntrega", homework.getDueDate());
                bdEscribir.insert("tablaDeberes", null, contentValues);
                String consulta = "SELECT id from tablaDeberes";
                Cursor cursor = bdLectura.rawQuery(consulta, null);

                // Obtener el ID del último registro insertado
                Cursor cursor2 = bdLectura.rawQuery("SELECT last_insert_rowid()", null);
                if (cursor != null && cursor.moveToFirst()) {
                    int id = cursor.getInt(0); // Obtiene el ID del último registro
                    homework.setId(id); // Asigna la ID al objeto homework
                }
                if (cursor != null) {
                    cursor.close();
                }

            } else {
                // Si es una tarea editada, se actualiza en la lista y en la base de datos
                int idEditar = homeworkToEdit.getId();

                homeworkList.set(homeworkList.indexOf(homeworkToEdit), homework); // Reemplaza la tarea editada

                String[] argumentos = new String[]{String.valueOf(idEditar)};
                contentValues.put("asignatura", homework.getSubject());
                contentValues.put("descripcion", homework.getDescription());
                contentValues.put("estadoTarea", homework.isCompleted());
                contentValues.put("fechaEntrega", homework.getDueDate());
                bdEscribir.update("tablaDeberes", contentValues, "id=?", argumentos);
            }
            adapter.notifyDataSetChanged(); // Notificar que los datos han cambiado
        });

        dialog.show(getSupportFragmentManager(), "AddHomeworkDialog");
//
//        AddHomeworkDialogFragment dialog = AddHomeworkDialogFragment.newInstance(homeworkToEdit);
//        dialog.setOnHomeworkSavedListener(homework -> {
//            if (homeworkToEdit == null) {
//                homeworkList.add(homework);
//            } else {
//                homeworkList.set(homeworkList.indexOf(homeworkToEdit), homework);
//            }
//            adapter.notifyDataSetChanged();
//        });
//        dialog.show(getSupportFragmentManager(), "AddHomeworkDialog");
    }

    private void showBottomSheet(Homework homework) {
        // Creación del diálogo
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

        // Inflar el layout del diálogo
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_homework_options, null);

        // Asignar acciones a los botones

        // Opción de editar
        view.findViewById(R.id.editOption).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showAddHomeworkDialog(homework);
        });

        // Opción de eliminar
        view.findViewById(R.id.deleteOption).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showDeleteConfirmation(homework);
        });


        // Opción de marcar como completada
        view.findViewById(R.id.completeOption).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            homework.setCompleted(true);
            contentValues.put("estadoTarea", homework.isCompleted());
            bdEscribir.update("tablaDeberes", contentValues,"id = ?", new String[]{String.valueOf(homework.getId())});
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Tarea marcada como completada", Toast.LENGTH_SHORT).show();
        });

        // Mostrar el diálogo
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void showDeleteConfirmation(Homework homework) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar este deber?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    bdEscribir.delete("tablaDeberes","id = ?", new String[]{String.valueOf(homework.getId())});
                    homeworkList.remove(homework);
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }



}
