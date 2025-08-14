# Receptor de dados do ESP32 via BLE (Android/Kotlin)
Este repositório contém o código-fonte de um aplicativo Android desenvolvido em Kotlin. O aplicativo funciona como um cliente Bluetooth Low Energy (BLE) para receber e exibir dados de um transmissor ESP32 customizado

Funcionalidades
 * Scan BLE: Procura por dispositivos BLE próximos que correspondam a um UUID de serviço específico.
 * Conexão Automática: Conecta-se automaticamente ao primeiro dispositivo encontrado que corresponda ao serviço esperado.
 * Recebimento de Dados: Inscreve-se para receber notificações de uma característica BLE e exibe os dados recebidos (atualmente, o tempo de pressionamento de um botão em segundos).
 * Interface Simples: Uma tela única que exibe o último valor recebido do ESP32.
 * Gerenciamento de Permissões: Solicita as permissões necessárias para Bluetooth e Localização em tempo de execução.

Funcionalidades
 * Scan BLE: Procura por dispositivos BLE próximos que correspondam a um UUID de serviço específico.
 * Conexão Automática: Conecta-se automaticamente ao primeiro dispositivo encontrado que corresponda ao serviço esperado.
 * Recebimento de Dados: Inscreve-se para receber notificações de uma característica BLE e exibe os dados recebidos (atualmente, o tempo de pressionamento de um botão em segundos).
 * Interface Simples: Uma tela única que exibe o último valor recebido do ESP32.
 * Gerenciamento de Permissões: Solicita as permissões necessárias para Bluetooth e Localização em tempo de execução.

Como compilar e usar
* Abra no Android Studio:
   * Abra o Android Studio.
   * Selecione Open e navegue até a pasta do projeto clonado.
 * Sincronize o Gradle:
   * Aguarde o Android Studio sincronizar e baixar todas as dependências do projeto (definidas no arquivo build.gradle.kts).
 * Execute o Aplicativo:
   * Conecte seu dispositivo Android físico ao computador.
   * Clique no botão "Run 'app'" (ícone de play verde) no Android Studio.
 * Teste:
   * Certifique-se de que o seu hardware ESP32 esteja ligado e anunciando o serviço BLE.
   * Abra o aplicativo no seu celular. Ele pedirá as permissões necessárias.
   * O aplicativo começará a escanear e se conectará automaticamente ao ESP32.
   * Pressione o botão no hardware. Ao soltá-lo, o valor correspondente deverá aparecer na tela do aplicativo.

Configuração
Para que o aplicativo se comunique com o seu ESP32, os UUIDs do serviço e da característica devem ser idênticos aos definidos no firmware do ESP32.
Você pode enctrar e alterar esses valores no código-fonte do aplicativo.



