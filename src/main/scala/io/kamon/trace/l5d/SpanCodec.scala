package io.kamon.trace.l5d

import java.util.Base64

import com.twitter.finagle.tracing.{SpanId, TraceId}
import com.twitter.util.Try
import kamon.Kamon
import kamon.context.{Codecs, Context, TextMap}
import kamon.trace.SpanContext.SamplingDecision
import kamon.trace.{IdentityProvider, Span, SpanContext}

object SpanCodec {
    class L5D extends Codecs.ForEntry[TextMap] {
      import L5D.Headers

      override def encode(context: Context): TextMap = {
        val span = context.get(Span.ContextKey)
        val carrier = TextMap.Default()

        if(span.nonEmpty()) {
          val spanContext = span.context()
          val traceId = SpanId.fromString(spanContext.traceID.string)
          val parentId = SpanId.fromString(spanContext.parentID.string)
          val spanId = SpanId.fromString(spanContext.spanID.string)

          val finalTraceId = TraceId(traceId, parentId, spanId.get, encodeSamplingDecision(spanContext.samplingDecision))
          carrier.put(Headers.L5DContextTrace, Base64.getEncoder.encodeToString(TraceId.serialize(finalTraceId)))
        }

        carrier
      }

      override def decode(carrier: TextMap, context: Context): Context = {
        val optionTryTraceId: Option[Try[TraceId]] = carrier.get(Headers.L5DContextTrace).map(x => TraceId.deserialize(Base64.getDecoder.decode(x)))
        val identityProvider = Kamon.tracer.identityProvider


        val traceID = optionTryTraceId.flatMap(t => t.toOption.map(x => x.traceId.toString()))
          .map(id => identityProvider.traceIdGenerator().from(id))
          .getOrElse(IdentityProvider.NoIdentifier)

        val spanID = optionTryTraceId.flatMap(t => t.toOption.map(x => x.spanId.toString()))
          .map(id => identityProvider.spanIdGenerator().from(id))
          .getOrElse(IdentityProvider.NoIdentifier)

        if(traceID != IdentityProvider.NoIdentifier && spanID != IdentityProvider.NoIdentifier) {
          val parentID = optionTryTraceId.flatMap(t => t.toOption.map(x => x.parentId.toString()))
            .map(id => identityProvider.spanIdGenerator().from(id))
            .getOrElse(IdentityProvider.NoIdentifier)

          val samplingDecision = optionTryTraceId.flatMap(t => t.toOption.map(x => x.sampled))
              .map(opt => opt.fold[SamplingDecision](SamplingDecision.Unknown)(if(_)SamplingDecision.Sample else SamplingDecision.DoNotSample))
                .getOrElse(SamplingDecision.Unknown)

          context.withKey(Span.ContextKey, Span.Remote(SpanContext(traceID, spanID, parentID, samplingDecision)))

        } else context
      }

      private def encodeSamplingDecision(samplingDecision: SamplingDecision): Option[Boolean] = samplingDecision match {
        case SamplingDecision.Sample      => Some(true)
        case SamplingDecision.DoNotSample => Some(false)
        case SamplingDecision.Unknown     => None
      }
    }

    object L5D {

      def apply(): L5D =
        new L5D()

      object Headers {
        val L5DContextTrace = "l5d-ctx-trace"
      }
    }
}
