"use client";

import { Camera, CameraOff } from "lucide-react";
import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from "react";

type DetectedBarcode = { rawValue: string };
type BarcodeDetectorLike = { detect: (source: CanvasImageSource) => Promise<DetectedBarcode[]> };
type BarcodeDetectorConstructor = new (options?: { formats?: string[] }) => BarcodeDetectorLike;

function getDetectorConstructor(): BarcodeDetectorConstructor | null {
  if (typeof window === "undefined") return null;
  return (window as unknown as { BarcodeDetector?: BarcodeDetectorConstructor }).BarcodeDetector ?? null;
}

export function QrScanner({ onScan }: { onScan: (value: string) => void }) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [active, setActive] = useState(false);
  const [error, setError] = useState("");

  const supported = useSyncExternalStore(
    () => () => {},
    () => getDetectorConstructor() !== null && !!navigator.mediaDevices,
    () => null,
  );

  const stop = useCallback(() => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    setActive(false);
  }, []);

  useEffect(() => stop, [stop]);

  async function start() {
    setError("");
    const Detector = getDetectorConstructor();
    if (!Detector) {
      setError("This browser cannot scan with the camera. Type or scan the code into the field instead.");
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } });
      streamRef.current = stream;
      setActive(true);

      const video = videoRef.current;
      if (!video) return;
      video.srcObject = stream;
      await video.play();

      const detector = new Detector({ formats: ["qr_code"] });
      const tick = async () => {
        if (!streamRef.current) return;
        try {
          const results = await detector.detect(video);
          const value = results[0]?.rawValue;
          if (value) {
            stop();
            onScan(value);
            return;
          }
        } catch {
        }
        requestAnimationFrame(() => void tick());
      };
      requestAnimationFrame(() => void tick());
    } catch {
      setError("Camera access was refused. Allow the camera, or enter the code by hand.");
      stop();
    }
  }

  return (
    <div>
      <div className="flex flex-wrap gap-3">
        {active ? (
          <button
            type="button"
            onClick={stop}
            className="flex items-center gap-2 rounded-md border border-line px-4 py-3 font-semibold text-muted hover:border-danger hover:text-danger"
          >
            <CameraOff size={18} aria-hidden />
            Stop camera
          </button>
        ) : (
          <button
            type="button"
            onClick={start}
            disabled={supported === false}
            className="flex items-center gap-2 rounded-md bg-accent px-4 py-3 font-semibold text-background disabled:cursor-not-allowed disabled:opacity-50"
          >
            <Camera size={18} aria-hidden />
            Scan with camera
          </button>
        )}
      </div>

      {supported === false && (
        <p className="mt-3 text-sm text-muted">
          Camera scanning is not available in this browser. A USB barcode scanner still works: focus the field below and scan.
        </p>
      )}
      {error && <p className="mt-3 rounded-md border border-danger/40 bg-danger/10 p-3 text-sm text-danger">{error}</p>}

      <div className={active ? "mt-4 overflow-hidden rounded-md border border-accent/40" : "hidden"}>
        <video ref={videoRef} className="aspect-video w-full bg-black object-cover" playsInline muted />
      </div>
    </div>
  );
}
