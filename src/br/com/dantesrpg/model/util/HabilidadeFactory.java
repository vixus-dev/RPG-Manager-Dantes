package br.com.dantesrpg.model.util;

import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.habilidades.*;
import br.com.dantesrpg.model.habilidades.boss.*;
import br.com.dantesrpg.model.habilidades.classe.*;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.TreeMap; // Para ordem alfabética

public class HabilidadeFactory {

	// Mapa que guarda "Nome da Habilidade" -> "Como criar ela"
	private static final Map<String, Supplier<Habilidade>> registro = new TreeMap<>();

	static {
		// Genéricas / Armas
		registro.put("Iaijutsu", Iaijutsu::new);
		registro.put("Passo Sombrio", PassoSombrio::new);
		registro.put("Rajada de Drone", RajadaDrone::new);
		registro.put("Rajada de Drone II", RajadaDrone2::new);
		registro.put("Investida Flamejante", InvestidaDeChamas::new);
		registro.put("Nano-Guarda", NanoGuarda::new);
		registro.put("Campo de Força", CampoDeForça::new);
		registro.put("Campo de Força II", CampoDeForça::new);
		registro.put("Cura I", Cura_Tier1::new);
		registro.put("Bombardeio", Bombardeio::new);
		registro.put("Investida Serra-Espada", InvestidaDeSerraEspada::new);
		registro.put("Rodar Serra-Espada", RodarDeSerraEspada::new);
		registro.put("Arremesso Serra-Espada", ArremessoDeSerraEspada::new);
		registro.put("DoubleDown", DoubleDown::new);
		registro.put("Arremesso de Orb", ArremessoDeOrb::new);
		registro.put("Pisotear", PisarOrb::new);
		registro.put("Bola de Fogo", BolaDeFogo::new);
		registro.put("Barragem de Espadas", LaminasDaJustiça::new);
		registro.put("Cura Suprema", CuraSuprema::new);
		registro.put("Mordida", Mordida::new);
		registro.put("Tecnica de Amplificação", TecnicaDeAmplificação::new);
		registro.put("MordidaQuente", MordidaQuente::new);
		registro.put("SLAM", SLAM::new);
		registro.put("SLAP", SLAP::new);
		registro.put("HOOK", HOOK::new);
		registro.put("Roubo de Essência", RouboDeEssencia::new);
		registro.put("Ceifa Ampla", DescargaEletrica::new);
		registro.put("Presença do Zero", PresencaDoZero::new);
		registro.put("Invocar Tentáculo", InvocarImp::new);
		registro.put("Puxar Essencia", PuxarEssencia::new);
		registro.put("Carga Dimensional", CargaDimensional::new);
		registro.put("Lâminas Espectrais", LaminasEspectrais::new);
		registro.put("Lightning Strike", LightningStrike::new);
		registro.put("Corte Temporal", CorteTemporal::new);
		registro.put("Arremesso de Machado", ArremessoDeMachad::new);
		registro.put("Sex in the air", SexInTheAir::new);
		registro.put("True love!", TrueLove::new);
		registro.put("Eternal Love!", EternalLove::new);
		registro.put("Invocar Imp", InvocarImp::new);
		registro.put("Projetil Explosivo", ProjetilExplosivo::new);
		registro.put("Pulo Pesado", PularStompLuxuria::new);
		registro.put("Chamar Reforços", ChamarReforços::new);
		registro.put("Descarga eletrica", DescargaEletrica::new);
		registro.put("Dilacerar", Dilacerar::new);

		// Classes
		registro.put("Técnica de Barreira", TecnicaDeBarreira::new);
		registro.put("Reversão de Feitiço", ReversaoDeFeitico::new);
		registro.put("Purificar", Purificar::new);
		registro.put("Bênção", Bencao::new);
		registro.put("Trocado", Trocado::new);
		registro.put("Combo!", Combo::new);
		registro.put("Ilusão", Ilusao::new);
		registro.put("Rezar", Rezar::new);
		registro.put("Auto-Guarda", NanoGuarda::new);
		registro.put("Raiva Imparável", RaivaImparavel::new);
		registro.put("Balanço Temerário", BalancoTemerario::new);
		registro.put("Grito de Guerra", GritoDeGuerra::new);
		registro.put("Golpe Devastador", GolpeDevastador::new);
	}

	public static Habilidade criarHabilidadePorNome(String nome) {
		if (nome == null || !registro.containsKey(nome)) {
			System.err.println("Factory: Habilidade '" + nome + "' não encontrada.");
			return null;
		}
		return registro.get(nome).get();
	}

	// Método novo para a UI do Editor
	public static Set<String> getNomesDisponiveis() {
		return registro.keySet();
	}
}
