package br.com.dantesrpg.controller.service;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.Amuleto;
import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.Armadura;
import br.com.dantesrpg.model.Classe;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Grimorio;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Item;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Raça;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.fantasmasnobres.AcertoDeContas;
import br.com.dantesrpg.model.fantasmasnobres.ApostadorIncansavel;
import br.com.dantesrpg.model.fantasmasnobres.InvocacaoMurasame;
import br.com.dantesrpg.model.fantasmasnobres.IraDeAnthyros;
import br.com.dantesrpg.model.fantasmasnobres.RingOfTheUndyingWill;
import br.com.dantesrpg.model.fantasmasnobres.RevelacaoDeYaweh;
import br.com.dantesrpg.model.fantasmasnobres.VigiliaEterna;
import br.com.dantesrpg.model.racas.Humano;
import br.com.dantesrpg.model.util.FileLoader;

public class PersonagemJsonService {

	private final CombatController controller;
	private final CatalogoItensService catalogoItensService;
	private final Function<String, Raça> racaMapper;
	private final Function<String, Classe> classeMapper;
	private final Function<String, FantasmaNobre> fantasmaNobreFactory;
	private final Supplier<Map<String, Map<String, Object>>> bestiarioSupplier;

	public PersonagemJsonService(CombatController controller, CatalogoItensService catalogoItensService,
			Function<String, Raça> racaMapper, Function<String, Classe> classeMapper,
			Function<String, FantasmaNobre> fantasmaNobreFactory,
			Supplier<Map<String, Map<String, Object>>> bestiarioSupplier) {
		this.controller = controller;
		this.catalogoItensService = catalogoItensService;
		this.racaMapper = racaMapper;
		this.classeMapper = classeMapper;
		this.fantasmaNobreFactory = fantasmaNobreFactory;
		this.bestiarioSupplier = bestiarioSupplier;
	}

	public Personagem carregarPersonagemComGson(String nomeArquivo) {
		Gson gson = new Gson();
		String resourcePath = "/data/players/" + nomeArquivo + ".json";

		try (InputStream is = FileLoader.carregarArquivo(resourcePath)) {
			if (is == null) {
				System.err.println("Arquivo JSON não encontrado: " + resourcePath);
				return null;
			}
			try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
				Type mapType = new TypeToken<Map<String, Object>>() {
				}.getType();
				Map<String, Object> data = gson.fromJson(reader, mapType);

				String nome = (String) data.getOrDefault("nome", "Nome Padrão");
				String nomeRaca = (String) data.getOrDefault("raca", "Placeholder");
				String nomeClasse = (String) data.getOrDefault("classe", "Placeholder");
				int nivel = ((Double) data.getOrDefault("nivel", 1.0)).intValue();
				int vidaMaximaBase = ((Double) data.getOrDefault("vidaMaximaBase", 10.0)).intValue();
				int iniciativaBase = ((Double) data.getOrDefault("iniciativaBase", 0.0)).intValue();
				int pontos = ((Double) data.getOrDefault("pontosParaDistribuir", 0.0)).intValue();
				int xpSalvo = data.containsKey("xpAtual") ? ((Double) data.get("xpAtual")).intValue() : 0;

				Map<Atributo, Integer> atributosBase = lerAtributosBase(nomeArquivo, data);
				Raça raca = racaMapper.apply(nomeRaca);
				Classe classe = classeMapper.apply(nomeClasse);
				if (raca == null || classe == null) {
					return null;
				}

				if (data.containsKey("RaçaV2")) {
					Object v2Flag = data.get("RaçaV2");
					if (v2Flag instanceof Boolean && (Boolean) v2Flag) {
						raca.setV2(true);
						System.out.println(">>> " + nome + " tem Raça V2 desbloqueada: " + raca.getNomeV2());
					}
				}

				int grau = data.containsKey("grau") ? ((Double) data.get("grau")).intValue() : 0;
				int segmentos = data.containsKey("segmentos") ? ((Double) data.get("segmentos")).intValue() : 0;

				Personagem personagem = new Personagem(nome, raca, classe, nivel, atributosBase, vidaMaximaBase,
						iniciativaBase);
				personagem.setXpAtual(xpSalvo);
				personagem.setGrau(grau);
				personagem.setSegmentosVida(segmentos);
				if (data.containsKey("wieldingMaximo")) {
					personagem.setWieldingMaximo(((Number) data.get("wieldingMaximo")).intValue());
				}
				personagem.setFaccao("JOGADOR");
				personagem.setPontosParaDistribuir(pontos);
				personagem.setJsonFileName(nomeArquivo + ".json");

				Arma armaPrincipal = carregarArmas(personagem, data);
				carregarEquipamentos(personagem, data);
				carregarInventario(personagem, data);
				carregarFantasmaNobre(personagem, nome, data);
				carregarDadosRaca(personagem, data);

				personagem.recalcularAtributosEstatisticas();
				carregarEstadoSessao(personagem, data);

				System.out.println("Personagem carregado: " + personagem.getNome() + " com Arma: "
						+ (armaPrincipal != null ? armaPrincipal.getNome() : "Nenhuma"));
				return personagem;
			}
		} catch (Exception e) {
			System.err.println("Erro Crítico ao ler ou processar JSON com Gson para: " + nomeArquivo);
			e.printStackTrace();
			return null;
		}
	}

	public void salvarPersonagem(Personagem personagem) {
		if (personagem == null || personagem.getJsonFileName() == null) {
			System.err.println("EDITOR: Falha ao salvar. Personagem ou nome do arquivo nulo.");
			return;
		}

		String nomeArquivo = personagem.getJsonFileName();
		System.out.println("EDITOR: Salvando " + nomeArquivo + "...");

		try {
			Map<String, Object> data = montarDadosSalvamento(personagem);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			String projectPath = System.getProperty("user.dir");
			String resourcePath = projectPath + "/src/main/resources/data/players/" + nomeArquivo;
			File file = new File(resourcePath);

			if (!file.getParentFile().exists()) {
				resourcePath = projectPath + "/src/data/players/" + nomeArquivo;
				file = new File(resourcePath);
			}

			file.getParentFile().mkdirs();

			try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
				gson.toJson(data, writer);
				System.out.println("SUCESSO: Arquivo salvo em: " + file.getAbsolutePath());

				URL urlBin = getClass().getResource("/data/players/" + nomeArquivo);
				if (urlBin != null) {
					File fileBin = new File(urlBin.toURI());
					try (Writer writerBin = new FileWriter(fileBin, StandardCharsets.UTF_8)) {
						gson.toJson(data, writerBin);
						System.out.println("HOTFIX: Salvo também na pasta BIN.");
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Erro CRÍTICO ao salvar JSON: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private Map<Atributo, Integer> lerAtributosBase(String nomeArquivo, Map<String, Object> data) {
		Map<String, Double> atributosJsonDouble = (Map<String, Double>) data.get("atributosBase");
		Map<Atributo, Integer> atributosBase = new HashMap<>();
		if (atributosJsonDouble != null) {
			for (Map.Entry<String, Double> entry : atributosJsonDouble.entrySet()) {
				try {
					atributosBase.put(Atributo.valueOf(entry.getKey().toUpperCase()), entry.getValue().intValue());
				} catch (IllegalArgumentException e) {
					System.err.println("Atributo inválido no JSON '" + nomeArquivo + "': " + entry.getKey());
				}
			}
		} else {
			System.err.println("Aviso: 'atributosBase' não encontrado ou inválido no JSON '" + nomeArquivo
					+ "'. Usando padrão.");
			for (Atributo atr : Atributo.values()) {
				atributosBase.putIfAbsent(atr, 1);
			}
		}
		return atributosBase;
	}

	private Arma carregarArmas(Personagem personagem, Map<String, Object> data) {
		Arma arma = null;
		List<Arma> armasEquipadas = new ArrayList<>();
		Object armasInfo = data.get("armasEquipadas");

		if (armasInfo instanceof List) {
			for (Object armaInfo : (List<?>) armasInfo) {
				Arma armaSalva = catalogoItensService.mapearArmaEquipadaSalva(armaInfo);
				if (armaSalva != null) {
					armasEquipadas.add(armaSalva);
				}
			}
		}
		if (armasEquipadas.isEmpty()) {
			arma = catalogoItensService.mapearArmaEquipadaSalva(data.get("armaEquipada"));
			if (arma != null) {
				armasEquipadas.add(arma);
			}
		} else {
			arma = armasEquipadas.get(0);
		}
		personagem.setArmasEquipadas(armasEquipadas);

		if (arma != null && data.containsKey("armaOverclock")) {
			arma.setGrauOverclock(((Number) data.get("armaOverclock")).intValue());
		}
		return arma;
	}

	private void carregarEquipamentos(Personagem personagem, Map<String, Object> data) {
		Armadura armadura = carregarArmadura(data);
		if (armadura != null) {
			personagem.setArmaduraEquipada(armadura);
			if (data.containsKey("armaduraOverclock")) {
				armadura.setGrauOverclock(((Number) data.get("armaduraOverclock")).intValue());
			}
		}

		Amuleto amuleto1 = carregarAmuleto(data.get("amuleto1"));
		if (amuleto1 != null) {
			personagem.setAmuleto1(amuleto1);
			if (data.containsKey("amuleto1Overclock")) {
				amuleto1.setGrauOverclock(((Number) data.get("amuleto1Overclock")).intValue());
			}
		}

		Amuleto amuleto2 = carregarAmuleto(data.get("amuleto2"));
		if (amuleto2 != null) {
			personagem.setAmuleto2(amuleto2);
			if (data.containsKey("amuleto2Overclock")) {
				amuleto2.setGrauOverclock(((Number) data.get("amuleto2Overclock")).intValue());
			}
		}
	}

	private Armadura carregarArmadura(Map<String, Object> data) {
		Object armaduraObj = data.get("armaduraEquipada");
		if (armaduraObj instanceof String) {
			Item item = getItem((String) armaduraObj);
			return item instanceof Armadura ? (Armadura) item : null;
		}
		if (armaduraObj instanceof Map) {
			return catalogoItensService.mapearArmadura((Map<String, Object>) armaduraObj);
		}
		return null;
	}

	private Amuleto carregarAmuleto(Object amuletoObj) {
		if (amuletoObj instanceof String) {
			Item item = getItem((String) amuletoObj);
			return item instanceof Amuleto ? (Amuleto) item : null;
		}
		if (amuletoObj instanceof Map) {
			return catalogoItensService.mapearAmuleto((Map<String, Object>) amuletoObj);
		}
		return null;
	}

	private void carregarInventario(Personagem personagem, Map<String, Object> data) {
		Map<String, ?> inventarioData = (Map<String, ?>) data.get("inventario");

		Map<String, Double> carteiraData = (Map<String, Double>) data.get("carteira");
		if (carteiraData != null) {
			if (carteiraData.containsKey("bronze"))
				personagem.getInventario().setMoedasBronze(carteiraData.get("bronze").intValue());
			if (carteiraData.containsKey("prata"))
				personagem.getInventario().setMoedasPrata(carteiraData.get("prata").intValue());
			if (carteiraData.containsKey("ouro"))
				personagem.getInventario().setMoedasOuro(carteiraData.get("ouro").intValue());
		}

		if (inventarioData != null) {
			for (Map.Entry<String, ?> entry : inventarioData.entrySet()) {
				String tipo = entry.getKey();

				if (!(entry.getValue() instanceof Number)) {
					System.err.println("Aviso: Quantidade de item inválida para " + tipo);
					continue;
				}

				int quantidade = ((Number) entry.getValue()).intValue();
				Item itemModelo = getItem(tipo);

				if (itemModelo != null) {
					for (int i = 0; i < quantidade; i++) {
						personagem.getInventario().adicionarItem(itemModelo);
					}
				}
			}

			if (data.containsKey("inventarioOverclock")) {
				Map<String, ?> ocRaw = (Map<String, ?>) data.get("inventarioOverclock");
				Map<String, Integer> ocData = new HashMap<>();
				for (Map.Entry<String, ?> ocEntry : ocRaw.entrySet()) {
					if (ocEntry.getValue() instanceof Number) {
						ocData.put(ocEntry.getKey(), ((Number) ocEntry.getValue()).intValue());
					}
				}
				personagem.getInventario().setOverclockData(ocData);
			}
		}
	}

	private void carregarFantasmaNobre(Personagem personagem, String nome, Map<String, Object> data) {
		if (data.containsKey("fantasmaNobre")) {
			String fnNome = (String) data.get("fantasmaNobre");
			FantasmaNobre fn = fantasmaNobreFactory.apply(fnNome);
			if (fn != null) {
				personagem.setFantasmaNobre(fn);
			}
			return;
		}

		if (nome.equals("Alexei")) {
			personagem.setFantasmaNobre(new RingOfTheUndyingWill());
		}
		if (nome.equals("Ayame")) {
			personagem.setFantasmaNobre(new InvocacaoMurasame());
		}
		if (nome.equals("Trakin")) {
			personagem.setFantasmaNobre(new AcertoDeContas());
		}
		if (nome.equals("Lyria")) {
			personagem.setFantasmaNobre(new ApostadorIncansavel());
		}
		if (nome.equals("Eidan")) {
			personagem.setFantasmaNobre(new VigiliaEterna());
		}
		if (nome.equals("Darrell")) {
			personagem.setFantasmaNobre(new br.com.dantesrpg.model.fantasmasnobres.ModoPolaris());
			personagem.adicionarHabilidadeExtra(new br.com.dantesrpg.model.habilidades.classe.DistortedSolo());
			personagem.adicionarHabilidadeExtra(new br.com.dantesrpg.model.habilidades.classe.WhaWhaSolo());
			personagem.adicionarHabilidadeExtra(new br.com.dantesrpg.model.habilidades.classe.PlainSolo());
		}
		if (nome.equals("Lilith")) {
			personagem.setFantasmaNobre(new br.com.dantesrpg.model.fantasmasnobres.InvocacaoSangrenta());
		}
		if (nome.equals("Arkos")) {
			personagem.setFantasmaNobre(new IraDeAnthyros());
		}
		if (nome.equals("KuangLi")) {
			personagem.setFantasmaNobre(new br.com.dantesrpg.model.fantasmasnobres.ProfetaDeBehemoth());
		}
		if (nome.equals("Pinocchio")) {
			personagem.setFantasmaNobre(new br.com.dantesrpg.model.fantasmasnobres.TheMastersCall());
		}
		if (nome.equalsIgnoreCase("Servant") || nome.equalsIgnoreCase("Sarvant")) {
			personagem.setFantasmaNobre(new RevelacaoDeYaweh());
		}
	}

	private void carregarDadosRaca(Personagem personagem, Map<String, Object> data) {
		if (!data.containsKey("racaData") || !(personagem.getRaca() instanceof Humano)) {
			return;
		}

		Map<String, Object> racaData = (Map<String, Object>) data.get("racaData");
		Humano humano = (Humano) personagem.getRaca();

		if (racaData.containsKey("vidaNegativaAcumulada")) {
			humano.setVidaNegativaAcumulada(((Number) racaData.get("vidaNegativaAcumulada")).doubleValue());
		}
		if (racaData.containsKey("estadoEmprestimo")) {
			String estadoStr = (String) racaData.get("estadoEmprestimo");
			humano.setEstadoAtual(Humano.EstadoEmprestimo.valueOf(estadoStr));
		}

		double valorAtivo = 0;
		double dividaAtivo = 0;
		if (racaData.containsKey("contratoAtivoValorTotal")) {
			valorAtivo = ((Number) racaData.get("contratoAtivoValorTotal")).doubleValue();
		}
		if (racaData.containsKey("contratoAtivoDividaRestante")) {
			dividaAtivo = ((Number) racaData.get("contratoAtivoDividaRestante")).doubleValue();
		}
		if (valorAtivo > 0) {
			br.com.dantesrpg.model.util.ContratoDeVida contratoAtivo = new br.com.dantesrpg.model.util.ContratoDeVida(
					br.com.dantesrpg.model.util.ContratoDeVida.FONTE_HUMANO, valorAtivo, -1, true);
			if (dividaAtivo > 0 && dividaAtivo < valorAtivo) {
				contratoAtivo.setDividaRestante(dividaAtivo);
			}
			personagem.getContratosDeVida().add(contratoAtivo);
		}

		if (racaData.containsKey("filaContratos")) {
			List<Double> lista = (List<Double>) racaData.get("filaContratos");
			humano.setFilaContratos(lista);
			if (lista != null) {
				for (Double pct : lista) {
					if (pct != null && pct > 0) {
						double valor = personagem.getVidaMaximaBase() * pct;
						br.com.dantesrpg.model.util.ContratoDeVida contratoFila = new br.com.dantesrpg.model.util.ContratoDeVida(
								br.com.dantesrpg.model.util.ContratoDeVida.FONTE_HUMANO, valor, -1, true);
						personagem.getContratosDeVida().add(contratoFila);
					}
				}
				humano.setFilaContratos(java.util.Collections.emptyList());
			}
		}
	}

	private void carregarEstadoSessao(Personagem personagem, Map<String, Object> data) {
		if (data.containsKey("vidaAtual")) {
			double vidaSalva = ((Number) data.get("vidaAtual")).doubleValue();
			personagem.setVidaAtual(vidaSalva, null, controller);
		} else {
			personagem.setVidaAtual(personagem.getVidaMaxima());
		}

		if (data.containsKey("manaAtual")) {
			double manaSalva = ((Number) data.get("manaAtual")).doubleValue();
			personagem.setManaAtual(manaSalva);
		} else {
			personagem.setManaAtual(personagem.getManaMaxima());
		}

		if (data.containsKey("escudoNormalAtual")) {
			personagem.setEscudoNormalAtual(((Number) data.get("escudoNormalAtual")).doubleValue());
		}
		if (data.containsKey("escudoSangueAtual")) {
			personagem.setEscudoSangueAtual(((Number) data.get("escudoSangueAtual")).doubleValue());
		}
		if (data.containsKey("escudoDivinoAtual")) {
			personagem.setEscudoDivinoAtual(((Number) data.get("escudoDivinoAtual")).doubleValue());
		}
		if (data.containsKey("escudoAtual")
				&& !data.containsKey("escudoNormalAtual")
				&& !data.containsKey("escudoSangueAtual")) {
			double escudoSalvo = ((Number) data.get("escudoAtual")).doubleValue();
			personagem.setEscudoAtual(escudoSalvo);
		}

		if (data.containsKey("contadorTU")) {
			personagem.setContadorTU(((Number) data.get("contadorTU")).intValue());
		}

		if (data.containsKey("posX") && data.containsKey("posY")) {
			personagem.setPosX(((Number) data.get("posX")).intValue());
			personagem.setPosY(((Number) data.get("posY")).intValue());
		}

		if (data.containsKey("faccao")) {
			personagem.setFaccao((String) data.get("faccao"));
		}
	}

	private Map<String, Object> montarDadosSalvamento(Personagem personagem) {
		Map<String, Object> data = new HashMap<>();

		data.put("nome", personagem.getNome());
		data.put("raca", personagem.getRaca().getNome());
		data.put("classe", personagem.getClasse().getNome());
		data.put("nivel", personagem.getNivel());
		data.put("xpAtual", personagem.getXpAtual());
		data.put("pontosParaDistribuir", personagem.getPontosParaDistribuir());
		data.put("vidaMaximaBase", personagem.getVidaMaximaBase());
		data.put("grau", personagem.getGrau());
		data.put("segmentos", personagem.getSegmentosVida());
		if (personagem.getRaca() != null && personagem.getRaca().isV2()) {
			data.put("RaçaV2", true);
		}
		if (personagem.getFantasmaNobre() != null) {
			data.put("fantasmaNobre", personagem.getFantasmaNobre().getClass().getSimpleName());
		}

		data.put("iniciativaBase", personagem.getIniciativaBase());

		data.put("atributosBase", personagem.getAtributosBase());
		data.put("wieldingMaximo", personagem.getWieldingMaximo());

		List<Object> armasSalvas = new ArrayList<>();
		for (Arma arma : personagem.getArmasEquipadas()) {
			armasSalvas.add(catalogoItensService.criarDadosArmaEquipada(arma));
		}
		if (!armasSalvas.isEmpty()) {
			data.put("armasEquipadas", armasSalvas);
		}
		salvarArmaLegada(personagem, data);
		salvarEquipamentos(personagem, data);
		salvarInventario(personagem, data);
		salvarEstadoSessao(personagem, data);
		salvarDadosHumano(personagem, data);

		return data;
	}

	private void salvarArmaLegada(Personagem personagem, Map<String, Object> data) {
		if (personagem.getArmaEquipada() == null) {
			return;
		}

		Arma arma = personagem.getArmaEquipada();
		if (arma instanceof Grimorio) {
			Grimorio grimorio = (Grimorio) arma;
			Map<String, Object> grimorioData = new HashMap<>();
			grimorioData.put("nome", grimorio.getNome());
			List<String> nomesMagias = grimorio.getMagiasArmazenadas().stream()
					.map(Habilidade::getNome)
					.collect(Collectors.toList());
			grimorioData.put("magiasSalvas", nomesMagias);
			data.put("armaEquipada", grimorioData);
		} else {
			data.put("armaEquipada", arma.getNome());
		}
		if (arma.getGrauOverclock() > 0) {
			data.put("armaOverclock", arma.getGrauOverclock());
		}
	}

	private void salvarEquipamentos(Personagem personagem, Map<String, Object> data) {
		if (personagem.getArmaduraEquipada() != null) {
			data.put("armaduraEquipada", personagem.getArmaduraEquipada().getNome());
			if (personagem.getArmaduraEquipada().getGrauOverclock() > 0) {
				data.put("armaduraOverclock", personagem.getArmaduraEquipada().getGrauOverclock());
			}
		}
		if (personagem.getAmuleto1() != null) {
			data.put("amuleto1", personagem.getAmuleto1().getNome());
			if (personagem.getAmuleto1().getGrauOverclock() > 0) {
				data.put("amuleto1Overclock", personagem.getAmuleto1().getGrauOverclock());
			}
		}
		if (personagem.getAmuleto2() != null) {
			data.put("amuleto2", personagem.getAmuleto2().getNome());
			if (personagem.getAmuleto2().getGrauOverclock() > 0) {
				data.put("amuleto2Overclock", personagem.getAmuleto2().getGrauOverclock());
			}
		}
	}

	private void salvarInventario(Personagem personagem, Map<String, Object> data) {
		data.put("inventario", personagem.getInventario().getItensAgrupados());

		Map<String, Integer> ocData = personagem.getInventario().getOverclockData();
		if (!ocData.isEmpty()) {
			data.put("inventarioOverclock", new HashMap<>(ocData));
		}

		Map<String, Integer> moedas = new HashMap<>();
		moedas.put("bronze", personagem.getInventario().getMoedasBronze());
		moedas.put("prata", personagem.getInventario().getMoedasPrata());
		moedas.put("ouro", personagem.getInventario().getMoedasOuro());
		data.put("carteira", moedas);
	}

	private void salvarEstadoSessao(Personagem personagem, Map<String, Object> data) {
		data.put("vidaAtual", personagem.getVidaAtual());
		data.put("manaAtual", personagem.getManaAtual());
		data.put("escudoNormalAtual", personagem.getEscudoNormalAtual());
		data.put("escudoSangueAtual", personagem.getEscudoSangueAtual());
		data.put("escudoDivinoAtual", personagem.getEscudoDivinoAtual());
		data.put("contadorTU", personagem.getContadorTU());
		data.put("posX", personagem.getPosX());
		data.put("posY", personagem.getPosY());
		data.put("faccao", personagem.getFaccao());
	}

	private void salvarDadosHumano(Personagem personagem, Map<String, Object> data) {
		if (!(personagem.getRaca() instanceof Humano)) {
			return;
		}

		Humano humano = (Humano) personagem.getRaca();
		Map<String, Object> dadosHumano = new HashMap<>();
		dadosHumano.put("vidaNegativaAcumulada", humano.getVidaNegativaAcumulada());
		dadosHumano.put("estadoEmprestimo", humano.getEstadoAtual().name());

		double valorAtivo = 0;
		double dividaAtivo = 0;
		List<Double> filaPcts = new ArrayList<>();
		double vidaBase = personagem.getVidaMaximaBase();
		boolean primeiro = true;
		for (br.com.dantesrpg.model.util.ContratoDeVida contrato : personagem.getContratosDeVida()) {
			if (contrato == null || !contrato.isHumano()) {
				continue;
			}
			if (primeiro) {
				valorAtivo = contrato.getValorTotal();
				dividaAtivo = contrato.getDividaRestante();
				primeiro = false;
			} else if (vidaBase > 0) {
				filaPcts.add(contrato.getValorTotal() / vidaBase);
			}
		}
		dadosHumano.put("filaContratos", filaPcts);
		dadosHumano.put("contratoAtivoValorTotal", valorAtivo);
		dadosHumano.put("contratoAtivoDividaRestante", dividaAtivo);
		data.put("racaData", dadosHumano);
	}

	private Item getItem(String tipoItem) {
		return catalogoItensService.getItem(tipoItem, bestiarioSupplier.get());
	}
}
