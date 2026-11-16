import java.util.Scanner;

public class Calculator {

    private static final String RESET_COMMAND = "c";
    private static final String EXIT_COMMAND = "s";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Калькулятор. Операции: +, -, *, /");
        System.out.println("Команды: C — сбросить результат, S — выйти");

        double result = 0;
        boolean hasResult = false;

        while (true) {
            if (!hasResult) {
                String firstInput = readToken(scanner, "Введите первое число: ");
                if (isExit(firstInput)) {
                    break;
                }
                if (isReset(firstInput)) {
                    System.out.println("Результат сброшен.");
                    continue;
                }
                if (!isNumber(firstInput)) {
                    System.out.println("Ошибка: '" + firstInput + "' - это не число.");
                    continue;
                }
                result = parseNumber(firstInput);
                hasResult = true;
            }

            String operationInput = readToken(scanner, "Введите операцию (+, -, *, /): ");
            if (isExit(operationInput)) {
                break;
            }
            if (isReset(operationInput)) {
                result = 0;
                hasResult = false;
                System.out.println("Результат сброшен.");
                continue;
            }
            if (!isOperation(operationInput)) {
                System.out.println("Ошибка: операция '" + operationInput + "' не поддерживается.");
                continue;
            }
            char operation = operationInput.charAt(0);

            String secondInput = readToken(scanner, "Введите второе число: ");
            if (isExit(secondInput)) {
                break;
            }
            if (isReset(secondInput)) {
                result = 0;
                hasResult = false;
                System.out.println("Результат сброшен.");
                continue;
            }
            if (!isNumber(secondInput)) {
                System.out.println("Ошибка: '" + secondInput + "' - это не число.");
                continue;
            }
            double secondOperand = parseNumber(secondInput);

            if (operation == '/' && secondOperand == 0) {
                System.out.println("Ошибка: делить на ноль нельзя.");
                continue;
            }
            result = calculate(result, operation, secondOperand);
            System.out.println("Результат: " + result);
        }

        System.out.println("Калькулятор выключен.");
        scanner.close();
    }

    // читаем строку, чтобы на месте числа можно было ввести C или S
    private static String readToken(Scanner scanner, String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNext()) {
            return EXIT_COMMAND;
        }
        return scanner.next();
    }

    private static boolean isExit(String input) {
        return input.equalsIgnoreCase(EXIT_COMMAND);
    }

    private static boolean isReset(String input) {
        return input.equalsIgnoreCase(RESET_COMMAND);
    }

    private static boolean isNumber(String input) {
        return input.matches("[-+]?\\d+([.,]\\d+)?");
    }

    private static double parseNumber(String input) {
        return Double.parseDouble(input.replace(',', '.'));
    }

    private static boolean isOperation(String input) {
        return input.equals("+") || input.equals("-") || input.equals("*") || input.equals("/");
    }

    private static double calculate(double firstOperand, char operation, double secondOperand) {
        switch (operation) {
            case '+':
                return firstOperand + secondOperand;
            case '-':
                return firstOperand - secondOperand;
            case '*':
                return firstOperand * secondOperand;
            case '/':
                return firstOperand / secondOperand;
            default:
                throw new IllegalArgumentException("Неподдерживаемая операция: " + operation);
        }
    }
}
