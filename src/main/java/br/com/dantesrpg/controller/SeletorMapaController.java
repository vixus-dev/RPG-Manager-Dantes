package br.com.dantesrpg.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import br.com.dantesrpg.controller.service.MapaCombateCoordinator;
import br.com.dantesrpg.model.map.MapGenerator;
import br.com.dantesrpg.model.map.TileDefinition;

public class SeletorMapaController {

    @FXML
    private ListView<String> listaMapas;
    @FXML
    private ComboBox<String> comboBioma;
    @FXML
    private ComboBox<String> comboTamanho;
    @FXML
    private ComboBox<String> comboDensidade;

    private MapaCombateCoordinator mapaCoordinator;
    private File pastaMapas;

    public void initData(MapaCombateCoordinator mapaCoordinator) {
        this.mapaCoordinator = mapaCoordinator;
        
        File opt1 = new File(System.getProperty("user.dir") + File.separator + "src" + File.separator + "main"
                + File.separator + "resources" + File.separator + "mapas");
        
        if (opt1.exists() && opt1.isDirectory()) {
            pastaMapas = opt1;
        } else {
            pastaMapas = new File(System.getProperty("user.dir") + File.separator + "src" + File.separator + "mapas");
        }
        
        carregarListaArquivos();
        
        comboBioma.setItems(FXCollections.observableArrayList("Superficie", "E.D.E.N", "E.D.E.N Segundo", "E.D.E.N Externo", "Limbo", "Templo Grego", "Inferno", "Clair De Lune", "Luxuria Interno", "Luxuria Externo", "Gula", "Gula Factory", "Ganacia Externo", "Ganacia Interno", "Exemple"));
        comboBioma.getSelectionModel().selectFirst();
        
        comboTamanho.setItems(FXCollections.observableArrayList("15x15 (Pequeno)", "25x25 (Médio)", "40x40 (Grande)", "60x60 (Gigante)"));
        comboTamanho.getSelectionModel().select(1);
        
        comboDensidade.setItems(FXCollections.observableArrayList("Baixa", "Média", "Alta"));
        comboDensidade.getSelectionModel().select(1);
    }

    private void carregarListaArquivos() {
        if (pastaMapas.exists() && pastaMapas.isDirectory()) {
            List<String> names = new ArrayList<>();
            adicionarArquivosRecursivamente(pastaMapas, "", names);
            listaMapas.setItems(FXCollections.observableArrayList(names));
        }
    }

    private void adicionarArquivosRecursivamente(File pasta, String prefixo, List<String> names) {
        File[] files = pasta.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    adicionarArquivosRecursivamente(f, prefixo + f.getName() + "/", names);
                } else {
                    String name = f.getName();
                    if (name.endsWith(".png") || name.endsWith(".jpg")) {
                        names.add(prefixo + name);
                    }
                }
            }
        }
    }

    @FXML
    private void onCarregarArquivoClick() {
        String selecionado = listaMapas.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            return;
        }
        File selectedFile = new File(pastaMapas, selecionado);
        mapaCoordinator.carregarNovaArenaLogic(selectedFile); // Supondo que usaremos o metodo direto
        fecharJanela();
    }

    @FXML
    private void onProcurarWindowsClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Abrir Imagem de Mapa");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Imagens de Mapa", "*.png", "*.jpg"));
        if (pastaMapas.exists()) fileChooser.setInitialDirectory(pastaMapas);
        
        File selectedFile = fileChooser.showOpenDialog(listaMapas.getScene().getWindow());
        if (selectedFile != null) {
            mapaCoordinator.carregarNovaArenaLogic(selectedFile);
            fecharJanela();
        }
    }

    @FXML
    private void onGerarMapaClick() {
        int indexTamanho = comboTamanho.getSelectionModel().getSelectedIndex();
        int size = indexTamanho == 0 ? 15 : (indexTamanho == 1 ? 25 : (indexTamanho == 2 ? 40 : 60));
        
        String bioma = comboBioma.getValue();
        String densidade = comboDensidade.getValue();
        
        // Chamada ao MapGenerator para criar o mapa logico na memoria
        TileDefinition[][] matriz = MapGenerator.gerarMapaProcedural(size, size, bioma, densidade);
        
        // Usa o callback que criaremos no Coordinator para receber a Matriz
        mapaCoordinator.carregarArenaProcedural(matriz);
        fecharJanela();
    }

    private void fecharJanela() {
        Stage stage = (Stage) listaMapas.getScene().getWindow();
        stage.close();
    }
}
