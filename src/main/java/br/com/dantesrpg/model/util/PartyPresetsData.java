package br.com.dantesrpg.model.util;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class PartyPresetsData {
	private String equippedSlotName;
	private List<PartyPreset> presets = new ArrayList<>();

	public String getEquippedSlotName() {
		return equippedSlotName;
	}

	public void setEquippedSlotName(String equippedSlotName) {
		this.equippedSlotName = equippedSlotName;
	}

	public List<PartyPreset> getPresets() {
		return presets;
	}

	public void setPresets(List<PartyPreset> presets) {
		this.presets = presets;
	}

	private static final String PRESETS_FILE_NAME = "party_presets.json";

	public static PartyPresetsData carregarPresets() {
		Gson gson = new Gson();
		String resourcePath = "/data/" + PRESETS_FILE_NAME;
		try (InputStream is = FileLoader.carregarArquivo(resourcePath)) {
			if (is == null) {
				return new PartyPresetsData();
			}
			try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
				PartyPresetsData loaded = gson.fromJson(reader, PartyPresetsData.class);
				return loaded != null ? loaded : new PartyPresetsData();
			}
		} catch (Exception e) {
			System.err.println("Erro ao carregar presets: " + e.getMessage());
			return new PartyPresetsData();
		}
	}

	public static void salvarPresets(PartyPresetsData presetsData) {
		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String projectPath = System.getProperty("user.dir");
			String resourcePath = projectPath + "/src/main/resources/data/" + PRESETS_FILE_NAME;
			File file = new File(resourcePath);

			if (!file.getParentFile().exists()) {
				resourcePath = projectPath + "/src/data/" + PRESETS_FILE_NAME;
				file = new File(resourcePath);
			}

			file.getParentFile().mkdirs();

			try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
				gson.toJson(presetsData, writer);
				System.out.println("SUCESSO: Presets salvos em: " + file.getAbsolutePath());

				URL urlBin = FileLoader.class.getResource("/data/" + PRESETS_FILE_NAME);
				if (urlBin != null) {
					File fileBin = new File(urlBin.toURI());
					try (Writer writerBin = new FileWriter(fileBin, StandardCharsets.UTF_8)) {
						gson.toJson(presetsData, writerBin);
						System.out.println("HOTFIX: Presets salvos também na pasta BIN.");
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Erro ao salvar presets: " + e.getMessage());
		}
	}
}
