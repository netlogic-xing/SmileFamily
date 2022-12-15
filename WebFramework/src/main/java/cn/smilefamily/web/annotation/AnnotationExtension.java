package cn.smilefamily.web.annotation;

import cn.smilefamily.annotation.AnnotationRegistry;
import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Configuration;
import cn.smilefamily.annotation.core.Scope;
import cn.smilefamily.extension.Extension;

import java.lang.annotation.Annotation;

public class AnnotationExtension implements Extension {

    private static final Scope REQ_SCOPE = new Scope() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return Scope.class;
        }

        @Override
        public String value() {
            return Request.REQUEST;
        }
    };
    private static final Scope SES_SCOPE = new Scope() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return Scope.class;
        }

        @Override
        public String value() {
            return Session.SESSION;
        }
    };
    private static final Bean CONTROLLER_BEAN = new Bean() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return Bean.class;
        }

        @Override
        public String value() {
            return "";
        }
    };

    @Override
    public String name() {
        return "AnnotationExtension";
    }

    @Override
    public void load() {
        AnnotationRegistry.register(Scope.class, Request.class, element -> REQ_SCOPE);
        AnnotationRegistry.register(Scope.class, Session.class, element -> SES_SCOPE);
        AnnotationRegistry.register(Bean.class, Controller.class, element -> CONTROLLER_BEAN);
        AnnotationRegistry.register(Configuration.class, WebConfiguration.class, element -> new Configuration() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Configuration.class;
            }

            @Override
            public String value() {
                return element.getAnnotation(WebConfiguration.class).value();
            }
        });
    }
}
