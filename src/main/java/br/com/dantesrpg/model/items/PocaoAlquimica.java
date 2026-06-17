package br.com.dantesrpg.model.items;

import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoEfeito;

import java.util.HashMap;
import java.util.Map;

public class PocaoAlquimica extends Consumivel {

	private final String tipoPocao;
	private final int inspiracaoCriador;
	private final boolean isV2;
	private final int rollAprimoramento;

	public PocaoAlquimica(String tipoId, String tipoPocao, int inspiracaoCriador) {
		this(tipoId, tipoPocao, inspiracaoCriador, false, 0);
	}

	public PocaoAlquimica(String tipoId, String tipoPocao, int inspiracaoCriador, boolean isV2, int rollAprimoramento) {
		super(tipoId,
				"Poção Alquímica" + (isV2 ? " V2" : "") + ": " + tipoPocao,
				gerarDescricao(tipoPocao, inspiracaoCriador, isV2, rollAprimoramento),
				0, // valorMoedas
				50, // custoTU
				true, // usavelEmCombate
				new HashMap<>()
		);
		this.tipoPocao = tipoPocao;
		this.inspiracaoCriador = inspiracaoCriador;
		this.isV2 = isV2;
		this.rollAprimoramento = rollAprimoramento;
	}

	private static String gerarDescricao(String tipo, int is, boolean isV2, int roll) {
		double mult = isV2 ? (1.0 + roll * 0.1) : 1.0;
		String suffix = isV2 ? " (Aprimorada V2: +" + (roll * 10) + "% de eficácia)" : "";
		switch (tipo) {
		case "Cura":
			double valorCura = (5 + is) * mult;
			return "Restaura " + String.format("%.1f", valorCura) + "% da vida máxima de quem consumir" + suffix + ".";
		case "Força":
			double valorForca = (10 + (4.5 * is)) * mult;
			return "Aumenta o bônus de dano de quem consumir em " + String.format("%.1f", valorForca) + "% por 200 TU" + suffix + ".";
		case "Velocidade":
			double valorVelocidade = is * mult;
			return "Reduz o TU de todas as habilidades de quem consumir em " + String.format("%.1f", valorVelocidade) + "% por 200 TU" + suffix + ".";
		case "Resistencia":
			double valorResistencia = is * mult;
			return "Aumenta a armadura de quem consumir em " + String.format("%.1f", valorResistencia) + "% do valor atual por 200 TU" + suffix + ".";
		case "Proteção":
			double valorEscudo = (10 + is) * mult;
			return "Cria um escudo com valor de " + String.format("%.1f", valorEscudo) + "% da vida máxima de quem consumir" + suffix + ".";
		default:
			return "Uma poção misteriosa.";
		}
	}

	public String getTipoPocao() {
		return tipoPocao;
	}

	public int getInspiracaoCriador() {
		return inspiracaoCriador;
	}

	public boolean isV2() {
		return isV2;
	}

	public int getRollAprimoramento() {
		return rollAprimoramento;
	}

	@Override
	public void usar(Personagem usuario, EstadoCombate estado,
			br.com.dantesrpg.controller.CombatController controller) {
		System.out.println(">>> " + usuario.getNome() + " usou " + getNome() + "!");
		int duracaoBuff = 200;
		double mult = isV2 ? (1.0 + rollAprimoramento * 0.1) : 1.0;

		switch (tipoPocao) {
		case "Cura":
			double percentualCura = ((5.0 + inspiracaoCriador) * mult) / 100.0;
			double valorCura = usuario.getVidaMaxima() * percentualCura;
			double vidaAntes = usuario.getVidaAtual();
			usuario.setVidaAtual(vidaAntes + valorCura, estado, controller);
			double curaReal = usuario.getVidaAtual() - vidaAntes;
			System.out.println(">>> " + usuario.getNome() + " recuperou " + (int) curaReal + " HP (Poção Alquímica" + (isV2 ? " V2" : "") + ").");
			break;

		case "Força":
			double valorForca = ((10.0 + (4.5 * inspiracaoCriador)) * mult) / 100.0;
			Map<String, Double> modsForca = new HashMap<>();
			modsForca.put("DANO_BONUS_PERCENTUAL", valorForca);
			Efeito buffForca = new Efeito("Buff " + getNome(), TipoEfeito.BUFF, duracaoBuff, modsForca, 0, 0);
			usuario.adicionarEfeito(buffForca);
			usuario.recalcularAtributosEstatisticas();
			System.out.println(">>> " + usuario.getNome() + " ganhou +" + (int) (valorForca * 100) + "% de bônus de dano por 200 TU.");
			break;

		case "Velocidade":
			double valorVelocidade = (inspiracaoCriador * mult) / 100.0;
			Map<String, Double> modsVelocidade = new HashMap<>();
			modsVelocidade.put("REDUCAO_TU_HABILIDADES", valorVelocidade);
			Efeito buffVelocidade = new Efeito("Buff " + getNome(), TipoEfeito.BUFF, duracaoBuff, modsVelocidade, 0, 0);
			usuario.adicionarEfeito(buffVelocidade);
			usuario.recalcularAtributosEstatisticas();
			System.out.println(">>> " + usuario.getNome() + " ganhou +" + (int) (valorVelocidade * 100) + "% de redução de TU em habilidades por 200 TU.");
			break;

		case "Resistencia":
			double valorResistencia = (inspiracaoCriador * mult) / 100.0;
			Map<String, Double> modsResistencia = new HashMap<>();
			modsResistencia.put("BONUS_ARMADURA_PERCENTUAL", valorResistencia);
			Efeito buffResistencia = new Efeito("Buff " + getNome(), TipoEfeito.BUFF, duracaoBuff, modsResistencia, 0, 0);
			usuario.adicionarEfeito(buffResistencia);
			usuario.recalcularAtributosEstatisticas();
			System.out.println(">>> " + usuario.getNome() + " ganhou +" + (int) (valorResistencia * 100) + "% de armadura atual por 200 TU.");
			break;

		case "Proteção":
			double percentualEscudo = ((10.0 + inspiracaoCriador) * mult) / 100.0;
			double valorEscudo = usuario.getVidaMaxima() * percentualEscudo;
			usuario.adicionarEscudoNormal(valorEscudo);
			System.out.println(">>> " + usuario.getNome() + " ganhou " + (int) valorEscudo + " de Escudo (Poção Alquímica" + (isV2 ? " V2" : "") + ").");
			break;
		}
	}
}
