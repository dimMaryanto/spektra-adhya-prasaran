package app.controller.penggajian;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import app.configs.BootFormInitializable;
import app.configs.DialogsFX;
import app.configs.FormatterFactory;
import app.entities.kepegawaian.KehadiranKaryawan;
import app.entities.kepegawaian.Penggajian;
import app.entities.kepegawaian.uang.prestasi.Motor;
import app.entities.kepegawaian.uang.prestasi.PembayaranCicilanMotor;
import app.entities.master.DataJabatan;
import app.entities.master.DataKaryawan;
import app.repositories.AbsensiService;
import app.repositories.CicilanMotorRepository;
import app.repositories.KaryawanService;
import app.repositories.PenggajianService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Callback;

@Component
public class PenggajianKaryawanPencairanDanaController implements BootFormInitializable {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private DialogsFX notif;
	private ApplicationContext springContext;

	@FXML
	private Button btnSave;
	@FXML
	private ComboBox<String> txtNip;
	@FXML
	private TextField txtNama;
	@FXML
	private TextField txtJabatan;
	@FXML
	private TextField txtJenisKelamin;
	@FXML
	private TextField txtGajiPokok;
	@FXML
	private TextField txtTotalKehadiran;
	@FXML
	private TextField txtJumlahKehadiran;
	@FXML
	private TextField txtJumlahLembur;
	@FXML
	private TextField txtTotalLembur;
	@FXML
	private TextField txtCicilanKe;
	@FXML
	private TextField txtMerekMotor;
	@FXML
	private TextField txtUangPrestasi;
	@FXML
	private TextField txtTotal;
	@FXML
	private CheckBox checkValid;

	@Autowired
	private PenggajianService servicePenggajian;

	@Autowired
	private KaryawanService serviceKaryawan;

	@Autowired
	private CicilanMotorRepository serviceCicilanMotor;

	@Autowired
	private FormatterFactory stringFormatter;

	@Autowired
	private AbsensiService serviceAbsen;

	private HashMap<String, DataKaryawan> mapKaryawan;
	private Penggajian penggajian;
	private List<KehadiranKaryawan> listTransport = new ArrayList<KehadiranKaryawan>();
	private List<KehadiranKaryawan> listLembur = new ArrayList<KehadiranKaryawan>();

	private PembayaranCicilanMotor pembayaranCicilanMotor;
	private Motor cicilanMotor;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		txtNip.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {

			@Override
			public ListCell<String> call(ListView<String> param) {
				return new ListCell<String>() {

					private DataKaryawan karyawan;

					@Override
					protected void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						if (empty) {
							setText(null);

						} else {
							this.karyawan = mapKaryawan.get(item);
							StringBuilder sb = new StringBuilder(karyawan.getNip()).append(" (")
									.append(karyawan.getNama()).append(" bagian ")
									.append(karyawan.getJabatan().getNama()).append(")");
							setText(sb.toString());
						}
					}
				};
			}
		});
		txtNip.getSelectionModel().selectedItemProperty().addListener((s, old, value) -> {
			if (value != null) {
				setFields(mapKaryawan.get(value));
			} else {
				clearFields();
			}
		});

	}

	private void setFields(DataKaryawan karyawan) {
		this.penggajian.setKaryawan(karyawan);

		LocalDate sekarang = LocalDate.now();
		LocalDate awalBulan = sekarang.withDayOfMonth(1);
		LocalDate akhirBulan = sekarang.withDayOfMonth(sekarang.lengthOfMonth());

		this.penggajian.setTahunBulan(stringFormatter.getDateIndonesionFormatterOnlyYearAndMonth(sekarang));

		DataJabatan jabatan = karyawan.getJabatan();

		txtNama.setText(karyawan.getNama());
		txtJabatan.setText(jabatan.getNama());
		txtJenisKelamin.setText(karyawan.getJenisKelamin().toString());

		this.penggajian.setGajiPokok(karyawan.getGajiPokok());
		txtGajiPokok.setText(stringFormatter.getCurrencyFormate(karyawan.getGajiPokok()));

		try {
			this.listTransport.clear();
			for (KehadiranKaryawan hadir : serviceAbsen.findByKaryawanAndTanggalHadirBetweenAndHadir(karyawan,
					Date.valueOf(awalBulan), Date.valueOf(akhirBulan), true)) {
				listTransport.add(hadir);
			}
			txtJumlahKehadiran.setText(stringFormatter.getNumberIntegerOnlyFormate(listTransport.size()));
			this.penggajian.setUangTransport(listTransport.size() * 30000D);
			txtTotalKehadiran.setText(stringFormatter.getCurrencyFormate(this.penggajian.getUangTransport()));
		} catch (Exception e) {
			logger.error("Tidak dapat mendapatkan data absensi karyawan atas nama {}", karyawan.getNama(), e);
		}

		try {
			this.listLembur.clear();
			for (KehadiranKaryawan lembur : serviceAbsen.findByKaryawanAndTanggalHadirBetweenAndLembur(karyawan,
					Date.valueOf(awalBulan), Date.valueOf(akhirBulan), true)) {
				this.listLembur.add(lembur);
			}
			txtJumlahLembur.setText(stringFormatter.getNumberIntegerOnlyFormate(listLembur.size()));
			this.penggajian.setUangLembur(listLembur.size() * 30000D);
			txtTotalLembur.setText(stringFormatter.getCurrencyFormate(this.penggajian.getUangLembur()));
		} catch (Exception e) {
			logger.error("Tidak dapat mendapatkan data lembur karyawan atas nama {}", karyawan.getNama(), e);
		}

		this.cicilanMotor = karyawan.getNgicilMotor();
		Double bayarCicilanMotor;
		if (cicilanMotor != null && cicilanMotor.isSetuju()) {
			this.pembayaranCicilanMotor = new PembayaranCicilanMotor();
			this.pembayaranCicilanMotor.setTanggalBayar(Date.valueOf(LocalDate.now()));
			this.pembayaranCicilanMotor.setAngsuranKe(cicilanMotor.getDaftarCicilan().size() + 1);
			this.pembayaranCicilanMotor.setBayar(cicilanMotor.getPembayaran());
			this.pembayaranCicilanMotor.setMotor(cicilanMotor);
			bayarCicilanMotor = this.pembayaranCicilanMotor.getBayar();

			txtCicilanKe.setText(
					stringFormatter.getNumberIntegerOnlyFormate(this.pembayaranCicilanMotor.getAngsuranKe()) + "x");
			txtMerekMotor.setText(cicilanMotor.getMerkMotor());
			txtUangPrestasi.setText(stringFormatter.getCurrencyFormate(pembayaranCicilanMotor.getBayar()));
		} else {
			bayarCicilanMotor = 0D;

			txtCicilanKe.clear();
			txtMerekMotor.clear();
			txtUangPrestasi.clear();
		}

		Double totalGaji = this.penggajian.getGajiPokok() + this.penggajian.getUangLembur()
				+ this.penggajian.getUangTransport() + bayarCicilanMotor;

		this.txtTotal.setText(stringFormatter.getCurrencyFormate(totalGaji));
	}

	private void clearFields() {
		txtNama.clear();
		txtJabatan.clear();
		txtJenisKelamin.clear();

		txtGajiPokok.clear();
		txtJumlahKehadiran.clear();
		txtJumlahLembur.clear();
	}

	@Override
	public Node initView() throws IOException {
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/scenes/inner/penggajian/PencairanDana.fxml"));
		loader.setController(springContext.getBean(this.getClass()));
		return loader.load();
	}

	@Override
	public void setStage(Stage stage) {

	}

	@Override
	public void initConstuct() {
		try {
			this.penggajian = new Penggajian();
			this.penggajian.setTanggal(Date.valueOf(LocalDate.now()));

			this.mapKaryawan = new HashMap<String, DataKaryawan>();
			txtNip.getItems().clear();
			for (DataKaryawan karyawan : serviceKaryawan.findAll()) {
				this.mapKaryawan.put(karyawan.getNip(), karyawan);
				this.txtNip.getItems().add(karyawan.getNip());
			}
		} catch (Exception e) {
			logger.error("Tidak dapat mendapatkan data karyawan yang belum menerima gaji pada bulan {}",
					LocalDate.now().toString());
			notif.showDefaultErrorLoad("Data karyawan", e);
			e.printStackTrace();
		}
	}

	@Override
	public void setNotificationDialog(DialogsFX notif) {
		this.notif = notif;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.springContext = applicationContext;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {

	}

	@Override
	public void initValidator() {

	}

	@FXML
	public void doSave(ActionEvent event) {

		servicePenggajian.save(this.penggajian);
		if (this.cicilanMotor != null) {
			serviceCicilanMotor.save(this.pembayaranCicilanMotor);
		}
		initConstuct();
	}

	@FXML
	public void doBack(ActionEvent event) {
	}

}
