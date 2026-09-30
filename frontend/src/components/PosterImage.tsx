import Image, { type ImageProps } from "next/image";

import { skipImageOptimization } from "@/lib/showcase";

export function PosterImage({ src, unoptimized, alt, ...props }: ImageProps & { src: string }) {
  return <Image src={src} alt={alt} unoptimized={unoptimized ?? skipImageOptimization(src)} {...props} />;
}
