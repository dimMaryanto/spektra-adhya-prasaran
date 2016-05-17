package app.controller.kasbon;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ResourceBundle;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import app.configs.BootInitializable;
import app.configs.CurrencyNumberFormatter;
import app.entities.KasbonKaryawan;
import app.entities.master.DataJabatan;
import app.entities.master.DataKaryawan;
import app.repositories.KaryawanService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

@Component
public class KasbonPeminjamanController implements BootInitializable {

	@Autowired
	private KaryawanService karyawanService;

	@Autowired
	private CurrencyNumberFormatter formatUang;

	private ApplicationContext springContext;
	@FXML
	private DatePicker txtTanggalTransaksi;
	@FXML
	private TableView<DataKaryawan> tableView;
	@FXML
	private TableColumn<DataKaryawan, String> columnNik;
	@FXML
	private TableColumn<DataKaryawan, String> columnNama;
	@FXML
	private TableColumn<DataKaryawan, String> columnJabatan;
	@FXML
	private TableColumn<DataKaryawan, String> columnCekSaldo;
	@FXML
	private TextField txtCariKaryawan;
	@FXML
	private Spinner<Double> txtPinjam;
	@FXML
	private Button btnSimpan;
	@FXML
	private TextField txtNominal;

	private KasbonKaryawan kasbon;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		txtTanggalTransaksi.getEditor().setAlignment(Pos.CENTER_RIGHT);
		txtTanggalTransaksi.setValue(LocalDate.now());

		txtPinjam.getEditor().setAlignment(Pos.CENTER_RIGHT);

		txtPinjam.setValueFactory(
				new SpinnerValueFactory.DoubleSpinnerValueFactory(Double.valueOf(0), Double.MAX_VALUE, 0, 50000));

		columnNik.setCellValueFactory(new PropertyValueFactory<DataKaryawan, String>("nik"));
		columnNama.setCellValueFactory(new PropertyValueFactory<DataKaryawan, String>("nama"));
		columnJabatan.setCellValueFactory(params -> {
			DataJabatan j = params.getValue().getJabatan();
			if (j != null) {
				return new SimpleStringProperty(j.getNama());
			} else {
				return new SimpleStringProperty();
			}
		});

		tableView.getSelectionModel().selectedItemProperty().addListener(
				(ObservableValue<? extends DataKaryawan> values, DataKaryawan oldValue, DataKaryawan newValue) -> {
					txtPinjam.setDisable(newValue == null);
				});

		txtPinjam.getValueFactory().valueProperty()
				.addListener((ObservableValue<? extends Double> values, Double oldValue, Double newValue) -> {
					btnSimpan.setDisable(newValue <= 0D);
					txtNominal.setText(formatUang.getCurrencyFormate(newValue));
				});
		btnSimpan.setOnAction(e -> {
			doSave(e);
		});

	}

	private void doSave(ActionEvent e) {
		DataKaryawan dataKaryawan = tableView.getSelectionModel().getSelectedItem();
		if (dataKaryawan != null) {
			this.kasbon = new KasbonKaryawan();

			kasbon.setKaryawan(dataKaryawan);
			kasbon.setTanggalPinjam(Date.valueOf(txtTanggalTransaksi.getValue()));
			kasbon.setPinjaman(txtPinjam.getValueFactory().getValue());
			kasbon.setPembayaran(0D);

			kasbon.setSaldoTerakhir(dataKaryawan.getTotalSaldoTerakhir() + kasbon.getPinjaman());

			dataKaryawan.getDaftarKasbon().add(kasbon);

			dataKaryawan.setSaldoTerakhir(kasbon.getSaldoTerakhir());

			karyawanService.save(dataKaryawan);

			initConstuct();
		} else {
			System.out.println("Table View belum dipilih");
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		this.springContext = arg0;
	}

	@Override
	public Node initView() throws IOException {
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/scenes/inner/kasbon/Peminjaman.fxml"));
		loader.setController(springContext.getBean(this.getClass()));
		return loader.load();
	}

	@Override
	public void setStage(Stage stage) {

	}

	@Override
	public void initConstuct() {
		this.kasbon = new KasbonKaryawan();
		txtTanggalTransaksi.setValue(LocalDate.now());
		tableView.getItems().clear();
		tableView.getItems().addAll(karyawanService.findAll());
		txtPinjam.getValueFactory().setValue(0D);
	}

	@FXML
	public void doBack() {

	}

}
