package br.com.dantesrpg.controller.service;

import java.util.Optional;

import br.com.dantesrpg.model.EstadoAndarParty;
import br.com.dantesrpg.model.enums.AndarCampanha;
import br.com.dantesrpg.model.theme.CatalogoTemasAndar;
import br.com.dantesrpg.model.theme.ConfiguracaoAndar;
import br.com.dantesrpg.model.util.PartyPreset;
import br.com.dantesrpg.model.util.PartyPresetsData;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class EstadoAndarService {
	private final CatalogoTemasAndar catalogo = new CatalogoTemasAndar();
	private final ObjectProperty<ConfiguracaoAndar> configuracaoAtual = new SimpleObjectProperty<>();

	public EstadoAndarService() {
		configuracaoAtual.set(catalogo.getConfiguracaoNula());
	}

	public void carregarDoPresetEquipado() {
		PartyPresetsData dados = PartyPresetsData.carregarPresets();
		Optional<PartyPreset> preset = buscarPresetEquipado(dados);
		if (preset.isEmpty()) {
			configuracaoAtual.set(catalogo.getConfiguracaoNula());
			return;
		}

		PartyPreset equipado = preset.get();
		EstadoAndarParty estado = new EstadoAndarParty(
				AndarCampanha.fromId(equipado.getAndarAtual()), equipado.getEstadoVisualAndar());
		configuracaoAtual.set(catalogo.buscarPorEstado(estado));
	}

	public void selecionarPorOpcao(String opcaoSeletor) {
		ConfiguracaoAndar configuracao = catalogo.buscarPorOpcao(opcaoSeletor);
		configuracaoAtual.set(configuracao);
		persistirNoPresetEquipado(configuracao);
	}

	public ConfiguracaoAndar getConfiguracaoAtual() {
		return configuracaoAtual.get();
	}

	public ReadOnlyObjectProperty<ConfiguracaoAndar> configuracaoAtualProperty() {
		return configuracaoAtual;
	}

	public java.util.List<String> getOpcoesSeletor() {
		return catalogo.getOpcoesSeletor();
	}

	private void persistirNoPresetEquipado(ConfiguracaoAndar configuracao) {
		PartyPresetsData dados = PartyPresetsData.carregarPresets();
		Optional<PartyPreset> preset = buscarPresetEquipado(dados);
		if (preset.isEmpty()) {
			return;
		}

		EstadoAndarParty estado = configuracao.getEstado();
		preset.get().setAndarAtual(estado.andar().name());
		preset.get().setEstadoVisualAndar(estado.estadoVisual());
		PartyPresetsData.salvarPresets(dados);
	}

	private Optional<PartyPreset> buscarPresetEquipado(PartyPresetsData dados) {
		if (dados == null || dados.getEquippedSlotName() == null || dados.getPresets() == null) {
			return Optional.empty();
		}
		return dados.getPresets().stream()
				.filter(preset -> preset != null && preset.getName() != null
						&& preset.getName().equalsIgnoreCase(dados.getEquippedSlotName()))
				.findFirst();
	}
}
