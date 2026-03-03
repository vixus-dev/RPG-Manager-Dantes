module RPG_DANTES_MK2 {
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;
	requires javafx.base;
	requires javafx.swing;
	requires com.google.gson;

	opens br.com.dantesrpg.controller to javafx.fxml;
	opens br.com.dantesrpg.main to javafx.fxml;

	opens br.com.dantesrpg.model to com.google.gson;
	opens br.com.dantesrpg.model.enums to com.google.gson;
	opens br.com.dantesrpg.model.map to com.google.gson;
	opens br.com.dantesrpg.model.items to com.google.gson;
	opens br.com.dantesrpg.model.armas.unicas to com.google.gson;

	exports br.com.dantesrpg.main;
}