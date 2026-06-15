package br.com.dantesrpg.model.items;

import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Item;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.Atributo;
import java.util.Map;

public class EssenciaInimigo extends Item {

	// Guarda os dados base do inimigo no momento da captura
	private String nomeInimigoOriginal;
	private Map<Atributo, Integer> atributosBaseInimigo;
	private double vidaMaximaBaseInimigo;
	private Arma armaInimigo;

	public EssenciaInimigo(Personagem inimigoMorto) {
		// O item no inventário será "Essência de Demônio", "Essência de Goblin Chefe", etc.
		super("Essência de " + inimigoMorto.getNome(),
				"A alma capturada de um " + inimigoMorto.getNome() + ". Pode ser usada pela Murasame.", 0, 
				false // NÃO é usável em combate (pelo menu de inventário normal)
		);

		this.nomeInimigoOriginal = inimigoMorto.getNome();
		this.atributosBaseInimigo = inimigoMorto.getAtributosBase();
		this.vidaMaximaBaseInimigo = inimigoMorto.getVidaMaximaBase();
		this.armaInimigo = inimigoMorto.getArmaEquipada();
	}

	// Getters para a Invocação ler
	public String getNomeInimigoOriginal() {
		return nomeInimigoOriginal;
	}

	public Map<Atributo, Integer> getAtributosBaseInimigo() {
		return atributosBaseInimigo;
	}

	public double getVidaMaximaBaseInimigo() {
		return vidaMaximaBaseInimigo;
	}

	public Arma getArmaInimigo() {
		return armaInimigo;
	}

	// Implementação obrigatória de Item (não faz nada)
	@Override
	public void usar(Personagem usuario, EstadoCombate estado,
			br.com.dantesrpg.controller.CombatController controller) {
		System.out.println(">>> " + getNome() + " não pode ser usado diretamente.");
	}

	@Override
	public int getCustoTU() {
		return 0; // Não é usável
	}

	@Override
	public String getTipo() {
		return this.getNome();
	}

	@Override
	public String toString() {
		return this.getNome();
	}
}