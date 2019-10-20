package ru.kbakaras.e2.service.rest;

/**
 * Отдельный класс для некритических исключений. Предназначен для обозначения
 * ситуаций, когда ошибки как таковой не случилось, но и действий по изменению
 * данных никаких предпринято не было. Например, в случае, когда предпринимается
 * повторная попытка переконвертации сообщения, и результат конвертации
 * идентичен текущему состоянию.
 */
public class ManageQueueSkipException extends ManageQueueException {
    public ManageQueueSkipException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ManageQueueSkipException(String s) {
        super(s);
    }
}
