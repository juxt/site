import NextLink from 'next/link';
import cn from 'classnames';
import {ExternalLink} from 'components/ExternalLink';
import {IconTwitter} from 'components/Icon/IconTwitter';
import {Logo} from 'components/Logo';
import {IconGitHub} from 'components/Icon/IconGitHub';

export function Footer() {
  const socialLinkClasses = 'hover:text-primary dark:text-primary-dark';
  return (
    <>
      <div className="self-stretch w-full sm:pl-0 lg:pl-80 sm:pr-0 2xl:pr-80 pl-0 pr-0">
        <div className="mx-auto w-full px-5 sm:px-12 md:px-12 pt-10 md:pt-12 lg:pt-10">
          <hr className="max-w-7xl mx-auto border-border dark:border-border-dark" />
        </div>
        <footer className="text-secondary dark:text-secondary-dark py-12 px-5 sm:px-12 md:px-12 sm:py-12 md:py-16 lg:py-14">
          <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-5 gap-x-12 gap-y-8 max-w-7xl mx-auto ">
            <ExternalLink
              href="https://juxt.pro"
              className="col-span-2 sm:col-span-1 justify-items-start w-44 text-left">
              <div className="w-28">
                <Logo />
              </div>
              <div className="text-xs text-left mt-2 pr-0.5">
                &copy;{new Date().getFullYear()}
              </div>
            </ExternalLink>
            <div className="flex flex-col">
              <FooterLink href="/learn" isHeader={true}>
                Learn Site
              </FooterLink>
              <FooterLink href="/learn/">Quick Start</FooterLink>
              <FooterLink href="/reference#installation">
                Installation
              </FooterLink>
            </div>
            <div className="flex flex-col">
              <FooterLink href="/reference" isHeader={true}>
                API Reference
              </FooterLink>
              <FooterLink href="/reference">Schema Directives</FooterLink>
              <FooterLink href="/reference/cli">Site CLI Reference</FooterLink>
            </div>
            <div className="flex flex-col sm:col-start-2 xl:col-start-4">
              <FooterLink href="/" isHeader={true}>
                Community
              </FooterLink>
              <FooterLink href="https://github.com/xtdb/xtdb">
                XTDB Github
              </FooterLink>
              <FooterLink href="https://juxt-oss.zulipchat.com/#narrow/stream/194466-xtdb">
                XTDB Zuplip
              </FooterLink>
              <FooterLink href="http://www.twitter.com/xtdb_com">
                XTDB Twitter
              </FooterLink>
              {/* <FooterLink href="/">Community Resources</FooterLink> */}
            </div>
            <div className="flex flex-col">
              <FooterLink isHeader={true}>More</FooterLink>
              <FooterLink href="https://juxt.pro">JUXT</FooterLink>
              <FooterLink href="https://xtdb.com">XTDB</FooterLink>
              <div className="flex flex-row mt-8 gap-x-2">
                <ExternalLink
                  href="https://twitter.com/juxtpro"
                  className={socialLinkClasses}>
                  <IconTwitter />
                </ExternalLink>
                <ExternalLink
                  href="https://github.com/juxt"
                  className={socialLinkClasses}>
                  <IconGitHub />
                </ExternalLink>
              </div>
            </div>
          </div>
        </footer>
      </div>
    </>
  );
}

function FooterLink({
  href,
  children,
  isHeader = false,
}: {
  href?: string;
  children: React.ReactNode;
  isHeader?: boolean;
}) {
  const classes = cn('border-b inline-block border-transparent', {
    'text-sm text-primary dark:text-primary-dark': !isHeader,
    'text-md text-secondary dark:text-secondary-dark my-2 font-bold': isHeader,
    'hover:border-gray-10': href,
  });

  if (!href) {
    return <div className={classes}>{children}</div>;
  }

  if (href.startsWith('https://')) {
    return (
      <div>
        <ExternalLink href={href} className={classes}>
          {children}
        </ExternalLink>
      </div>
    );
  }

  return (
    <div>
      <NextLink href={href}>
        <a className={classes}>{children}</a>
      </NextLink>
    </div>
  );
}
